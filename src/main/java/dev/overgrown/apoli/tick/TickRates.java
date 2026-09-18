package dev.overgrown.apoli.tick;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.TickRateS2C;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TickRates {

    public static final int NORMAL = -1;
    public static final int CLEAR_ALL = -1;
    public static final int MAX_RATE = 10000;

    private record Owned(WeakReference<Entity> owner, TickState state) {}

    private static final Int2ObjectOpenHashMap<Owned> ENTITIES = new Int2ObjectOpenHashMap<>();
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<TickState>> CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, TickState> DIMENSIONS = new HashMap<>();

    private static final Int2IntOpenHashMap SYNCED = new Int2IntOpenHashMap();

    private static long currentTick = -1L;
    private static int baseRate = 20;
    private static boolean anyChunkOrDimension;

    private static boolean serverOverridden;
    private static float savedRate = 20.0F;
    private static boolean savedFrozen;
    private static long serverExpiry = TickState.PERMANENT;

    static {
        SYNCED.defaultReturnValue(Integer.MIN_VALUE);
    }

    private TickRates() {}

    public static boolean idle() {
        return ENTITIES.isEmpty() && !anyChunkOrDimension;
    }

    public static void clear() {
        ENTITIES.clear();
        CHUNKS.clear();
        DIMENSIONS.clear();
        SYNCED.clear();
        currentTick = -1L;
        baseRate = 20;
        anyChunkOrDimension = false;
        serverOverridden = false;
        savedRate = 20.0F;
        savedFrozen = false;
        serverExpiry = TickState.PERMANENT;
    }

    public static void serverTick(MinecraftServer server) {
        if (serverOverridden && serverExpiry != TickState.PERMANENT
            && server.getTickCount() >= serverExpiry) {
            releaseServer(server);
        }
        if (!idle()) sync(server);
        if (idle() && !SYNCED.isEmpty()) {
            SYNCED.clear();
            ApoliNetwork.broadcastTickRate(server, new TickRateS2C(CLEAR_ALL, NORMAL, baseRate));
        }
    }

    public static void requestServer(MinecraftServer server, float rate, boolean frozen, int duration) {
        if (!serverOverridden) {
            savedRate = TickRateVanilla.exactRate(server);
            savedFrozen = TickRateVanilla.frozen(server);
            serverOverridden = true;
        }
        serverExpiry = duration <= 0 ? TickState.PERMANENT : server.getTickCount() + duration;
        TickRateVanilla.setRate(server, rate);
        TickRateVanilla.setFrozen(server, frozen);
    }

    public static void releaseServer(MinecraftServer server) {
        if (!serverOverridden) return;
        serverOverridden = false;
        serverExpiry = TickState.PERMANENT;
        TickRateVanilla.setRate(server, savedRate);
        TickRateVanilla.setFrozen(server, savedFrozen);
    }

    public static int baseRate() {
        return baseRate;
    }

    private static void markScopes() {
        anyChunkOrDimension = !CHUNKS.isEmpty() || !DIMENSIONS.isEmpty();
    }

    private static void sync(MinecraftServer server) {
        long now = server.getTickCount();
        if (now == currentTick) return;
        currentTick = now;
        baseRate = TickRateVanilla.rate(server);
        advance(now);
    }

    private static void advance(long now) {
        Iterator<Owned> entities = ENTITIES.values().iterator();
        while (entities.hasNext()) {
            Owned owned = entities.next();
            if (owned.state().expired(now)) {
                entities.remove();
                Entity entity = owned.owner().get();
                if (entity != null && !entity.isRemoved()) pushToTrackers(entity, NORMAL);
            } else {
                owned.state().countDown();
            }
        }
        Iterator<Map.Entry<ResourceKey<Level>, Long2ObjectOpenHashMap<TickState>>> byLevel =
            CHUNKS.entrySet().iterator();
        while (byLevel.hasNext()) {
            Long2ObjectOpenHashMap<TickState> byChunk = byLevel.next().getValue();
            Iterator<TickState> chunks = byChunk.values().iterator();
            while (chunks.hasNext()) {
                TickState state = chunks.next();
                if (state.expired(now)) chunks.remove();
                else state.countDown();
            }
            if (byChunk.isEmpty()) byLevel.remove();
        }
        Iterator<TickState> dimensions = DIMENSIONS.values().iterator();
        while (dimensions.hasNext()) {
            TickState state = dimensions.next();
            if (state.expired(now)) dimensions.remove();
            else state.countDown();
        }
        markScopes();
    }

    public static TickState stateFor(Entity entity, TickScope scope) {
        return switch (scope) {
            case ENTITY, SERVER -> entityState(entity);
            case CHUNK -> chunkState(entity.level().dimension(), entity.chunkPosition().toLong());
            case DIMENSION -> dimensionState(entity.level().dimension());
        };
    }

    public static TickState entityState(Entity entity) {
        TickState existing = lookupEntity(entity);
        if (existing != null) return existing;
        TickState state = new TickState();
        ENTITIES.put(entity.getId(), new Owned(new WeakReference<>(entity), state));
        return state;
    }

    private static @Nullable TickState lookupEntity(Entity entity) {
        if (ENTITIES.isEmpty()) return null;
        int id = entity.getId();
        Owned owned = ENTITIES.get(id);
        if (owned == null) return null;
        if (owned.owner().get() != entity) {
            ENTITIES.remove(id);
            return null;
        }
        return owned.state();
    }

    public static TickState chunkState(ResourceKey<Level> dimension, long chunkPos) {
        Long2ObjectOpenHashMap<TickState> byChunk = CHUNKS.computeIfAbsent(dimension,
            key -> new Long2ObjectOpenHashMap<>());
        TickState state = byChunk.get(chunkPos);
        if (state == null) {
            state = new TickState();
            byChunk.put(chunkPos, state);
        }
        anyChunkOrDimension = true;
        return state;
    }

    public static TickState dimensionState(ResourceKey<Level> dimension) {
        anyChunkOrDimension = true;
        return DIMENSIONS.computeIfAbsent(dimension, key -> new TickState());
    }

    public static void exempt(Entity entity, long expiry) {
        TickState state = entityState(entity);
        if (state.rate() != TickState.INHERIT || state.frozen()) return;
        state.setExempt(true);
        state.keepUntil(expiry);
    }

    public static void clearEntity(Entity entity) {
        ENTITIES.remove(entity.getId());
    }

    public static void clearChunk(ResourceKey<Level> dimension, long chunkPos) {
        Long2ObjectOpenHashMap<TickState> byChunk = CHUNKS.get(dimension);
        if (byChunk == null) return;
        byChunk.remove(chunkPos);
        if (byChunk.isEmpty()) CHUNKS.remove(dimension);
        markScopes();
    }

    public static void clearDimension(ResourceKey<Level> dimension) {
        DIMENSIONS.remove(dimension);
        markScopes();
    }

    public static void prune(TickScope scope, Entity entity) {
        switch (scope) {
            case ENTITY, SERVER -> {
                TickState state = lookupEntity(entity);
                if (state != null && state.isDefault()) clearEntity(entity);
            }
            case CHUNK -> {
                ResourceKey<Level> dim = entity.level().dimension();
                long pos = entity.chunkPosition().toLong();
                Long2ObjectOpenHashMap<TickState> byChunk = CHUNKS.get(dim);
                if (byChunk != null) {
                    TickState state = byChunk.get(pos);
                    if (state != null && state.isDefault()) clearChunk(dim, pos);
                }
            }
            case DIMENSION -> {
                ResourceKey<Level> dim = entity.level().dimension();
                TickState state = DIMENSIONS.get(dim);
                if (state != null && state.isDefault()) clearDimension(dim);
            }
        }
    }

    @Nullable
    private static TickState lookupChunk(ResourceKey<Level> dimension, long chunkPos) {
        if (CHUNKS.isEmpty()) return null;
        Long2ObjectOpenHashMap<TickState> byChunk = CHUNKS.get(dimension);
        return byChunk == null ? null : byChunk.get(chunkPos);
    }

    private static int resolveEntity(Entity entity) {
        Entity root = entity.isPassenger() ? entity.getRootVehicle() : entity;
        int rate = NORMAL;
        TickState own = lookupEntity(root);
        if (own != null && !own.isDefault()) {
            rate = own.effectiveRate(baseRate);
        } else if (anyChunkOrDimension) {
            rate = resolveChunk(root.level(), root.chunkPosition().toLong());
        }
        pushToTrackers(entity, rate);
        return rate;
    }

    public static int resolveChunk(Level level, long chunkPos) {
        TickState own = lookupChunk(level.dimension(), chunkPos);
        if (own != null && !own.isDefault()) return own.effectiveRate(baseRate);
        return resolveDimension(level);
    }

    public static int resolveDimension(Level level) {
        if (DIMENSIONS.isEmpty()) return NORMAL;
        TickState own = DIMENSIONS.get(level.dimension());
        return own == null || own.isDefault() ? NORMAL : own.effectiveRate(baseRate);
    }

    private static void pushToTrackers(Entity entity, int rate) {
        int id = entity.getId();
        int previous = SYNCED.get(id);
        if (previous == rate) return;
        if (rate == NORMAL) {
            if (previous == Integer.MIN_VALUE) return;
            SYNCED.remove(id);
        } else {
            SYNCED.put(id, rate);
        }
        ApoliNetwork.sendTickRateToTrackers(entity, new TickRateS2C(id, rate, baseRate));
    }

    public static boolean shouldTickEntity(Entity entity) {
        if (idle()) return true;
        MinecraftServer server = entity.getServer();
        if (server == null) return true;
        sync(server);
        int rate = resolveEntity(entity);
        if (rate == NORMAL) return true;
        if (!gate(rate, baseRate, entity.level().getGameTime())) return false;
        if (rate > 0 && !(entity instanceof LivingEntity)) entity.hasImpulse = true;
        return true;
    }

    public static boolean shouldTickChunk(Level level, long chunkPos) {
        if (!anyChunkOrDimension) return true;
        MinecraftServer server = level.getServer();
        if (server == null) return true;
        sync(server);
        int rate = resolveChunk(level, chunkPos);
        return rate == NORMAL || gate(rate, baseRate, level.getGameTime());
    }

    public static boolean gate(int rate, int base, long tick) {
        if (rate >= base) return true;
        if (rate <= 0) return false;
        return Math.floorDiv(tick * rate, (long) base) != Math.floorDiv((tick - 1L) * rate, (long) base);
    }

    public static int effectiveRate(Entity entity, TickScope scope) {
        MinecraftServer server = entity.getServer();
        int base = TickRateVanilla.rate(server);
        return switch (scope) {
            case ENTITY -> {
                Entity root = entity.isPassenger() ? entity.getRootVehicle() : entity;
                TickState own = lookupEntity(root);
                if (own != null && !own.isDefault()) yield own.effectiveRate(base);
                yield effectiveChunkRate(root.level(), root.chunkPosition().toLong(), base);
            }
            case CHUNK -> effectiveChunkRate(entity.level(), entity.chunkPosition().toLong(), base);
            case DIMENSION -> effectiveDimensionRate(entity.level(), base);
            case SERVER -> TickRateVanilla.frozen(server) ? 0 : base;
        };
    }

    public static int effectiveChunkRate(Level level, long chunkPos, int base) {
        TickState own = lookupChunk(level.dimension(), chunkPos);
        if (own != null && !own.isDefault()) return own.effectiveRate(base);
        return effectiveDimensionRate(level, base);
    }

    public static int effectiveDimensionRate(Level level, int base) {
        if (DIMENSIONS.isEmpty()) return base;
        TickState own = DIMENSIONS.get(level.dimension());
        return own == null ? base : own.effectiveRate(base);
    }

    public static @Nullable TickState peekEntity(Entity entity) {
        return lookupEntity(entity);
    }

    public static @Nullable TickState peekChunk(ResourceKey<Level> dimension, long chunkPos) {
        return lookupChunk(dimension, chunkPos);
    }
}
