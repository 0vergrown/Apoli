package dev.overgrown.apoli.block;

import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.builtin.block.GlowBlockAction;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.BlockGlowStyle;
import dev.overgrown.apoli.network.payload.BlockGlowsS2C;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BlockGlows {

    private static final int CHECK_INTERVAL = 20;
    private static final int VISIBILITY_INTERVAL = 5;

    private BlockGlows() {}

    private record Glow(BlockPos pos, Block block, GlowBlockAction.Cfg spec, long expiresAt) {}

    private record Key(ResourceKey<Level> dimension, long pos) {}

    private static final class Dimension {
        final Long2ObjectOpenHashMap<Glow> glows = new Long2ObjectOpenHashMap<>();
        final IdentityHashMap<GlowBlockAction.Cfg, int[]> specs = new IdentityHashMap<>();
        final List<Glow> added = new ArrayList<>();
        final List<Glow> removed = new ArrayList<>();
        @Nullable BlockGlowSavedData data;

        void retain(GlowBlockAction.Cfg spec) {
            specs.computeIfAbsent(spec, s -> new int[1])[0]++;
        }

        void release(GlowBlockAction.Cfg spec) {
            int[] count = specs.get(spec);
            if (count == null) return;
            if (--count[0] <= 0) specs.remove(spec);
        }

        void markDirty() {
            if (data != null) data.setDirty();
        }

        boolean idle() {
            return glows.isEmpty() && added.isEmpty() && removed.isEmpty();
        }
    }

    private static final class Viewer {
        @Nullable ResourceKey<Level> dimension;
        final IdentityHashMap<GlowBlockAction.Cfg, Boolean> sees = new IdentityHashMap<>();
        long seen;
    }

    private static final Map<ResourceKey<Level>, Dimension> DIMENSIONS = new HashMap<>();
    private static final Map<UUID, Viewer> VIEWERS = new HashMap<>();
    private static final Map<Long, List<Key>> EXPIRY = new HashMap<>();
    private static boolean warnedEncode;

    public static void glow(ServerLevel level, BlockPos pos, GlowBlockAction.Cfg spec) {
        Dimension dimension = dimension(level);
        long key = pos.asLong();
        long now = clock(level.getServer());
        long expiresAt = spec.duration() <= 0 ? -1L : now + spec.duration();
        Glow glow = new Glow(pos.immutable(), level.getBlockState(pos).getBlock(), spec, expiresAt);
        Glow previous = dimension.glows.put(key, glow);
        if (previous != null) {
            dimension.release(previous.spec());
            dimension.removed.add(previous);
        }
        dimension.retain(spec);
        dimension.added.add(glow);
        if (expiresAt >= 0) EXPIRY.computeIfAbsent(expiresAt, t -> new ArrayList<>(1)).add(new Key(level.dimension(), key));
        dimension.markDirty();
    }

    public static void tick(MinecraftServer server) {
        if (DIMENSIONS.isEmpty()) {
            if (!VIEWERS.isEmpty()) VIEWERS.clear();
            return;
        }
        long now = clock(server);
        expire(now);
        if (now % CHECK_INTERVAL == 0) sweep(server);
        sync(server, now);
        DIMENSIONS.values().removeIf(dimension -> {
            dimension.added.clear();
            dimension.removed.clear();
            return dimension.idle();
        });
    }

    private static void expire(long now) {
        List<Key> due = EXPIRY.remove(now);
        if (due == null) return;
        for (Key key : due) {
            Dimension dimension = DIMENSIONS.get(key.dimension());
            if (dimension == null) continue;
            Glow glow = dimension.glows.get(key.pos());
            if (glow == null || glow.expiresAt() != now) continue;
            dimension.glows.remove(key.pos());
            dimension.release(glow.spec());
            dimension.markDirty();
        }
    }

    private static void sweep(MinecraftServer server) {
        for (Map.Entry<ResourceKey<Level>, Dimension> entry : DIMENSIONS.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;
            Dimension dimension = entry.getValue();
            var iterator = dimension.glows.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Glow glow = iterator.next().getValue();
                if (!level.isLoaded(glow.pos())) continue;
                if (level.getBlockState(glow.pos()).getBlock() == glow.block()) continue;
                iterator.remove();
                dimension.release(glow.spec());
                dimension.removed.add(glow);
                dimension.markDirty();
            }
        }
    }

    private static void sync(MinecraftServer server, long now) {
        boolean visibilityPass = now % VISIBILITY_INTERVAL == 0;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int p = 0, n = players.size(); p < n; p++) {
            ServerPlayer player = players.get(p);
            Viewer viewer = VIEWERS.computeIfAbsent(player.getUUID(), id -> new Viewer());
            viewer.seen = now;
            ResourceKey<Level> where = player.level().dimension();
            boolean reset = viewer.dimension != where;
            if (reset) {
                viewer.dimension = where;
                viewer.sees.clear();
            }
            Dimension dimension = DIMENSIONS.get(where);
            if (dimension == null) continue;
            List<BlockGlowsS2C.Entry> adds = new ArrayList<>();
            LongArrayList removes = new LongArrayList();
            for (Glow glow : dimension.removed) {
                if (Boolean.TRUE.equals(viewer.sees.get(glow.spec()))) removes.add(glow.pos().asLong());
            }
            IdentityHashMap<GlowBlockAction.Cfg, Boolean> shown = null;
            if (reset || visibilityPass) {
                EntityCtx ctx = null;
                for (GlowBlockAction.Cfg spec : dimension.specs.keySet()) {
                    boolean visible = true;
                    if (spec.entityCondition().isPresent()) {
                        if (ctx == null) ctx = EntityCtx.of(player, player.level());
                        visible = spec.entityCondition().get().test(ctx);
                    }
                    Boolean before = viewer.sees.put(spec, visible);
                    boolean was = Boolean.TRUE.equals(before);
                    if (visible == was) continue;
                    if (visible && shown == null) shown = new IdentityHashMap<>();
                    if (visible) shown.put(spec, Boolean.TRUE);
                    for (Glow glow : dimension.glows.values()) {
                        if (glow.spec() != spec) continue;
                        if (visible) adds.add(entry(glow, now));
                        else removes.add(glow.pos().asLong());
                    }
                }
                viewer.sees.keySet().retainAll(dimension.specs.keySet());
            }
            if (!dimension.added.isEmpty()) {
                EntityCtx ctx = null;
                for (Glow glow : dimension.added) {
                    if (shown != null && shown.containsKey(glow.spec())) continue;
                    if (dimension.glows.get(glow.pos().asLong()) != glow) continue;
                    Boolean sees = viewer.sees.get(glow.spec());
                    if (sees == null) {
                        boolean visible = true;
                        if (glow.spec().entityCondition().isPresent()) {
                            if (ctx == null) ctx = EntityCtx.of(player, player.level());
                            visible = glow.spec().entityCondition().get().test(ctx);
                        }
                        viewer.sees.put(glow.spec(), visible);
                        sees = visible;
                    }
                    if (sees) adds.add(entry(glow, now));
                }
            }
            send(player, reset, adds, removes);
        }
        if (VIEWERS.size() != players.size()) VIEWERS.values().removeIf(viewer -> viewer.seen != now);
    }

    private static BlockGlowsS2C.Entry entry(Glow glow, long now) {
        BlockGlowStyle style = glow.spec().style();
        int remaining = glow.expiresAt() < 0 ? -1 : (int) Math.max(1L, glow.expiresAt() - now);
        return new BlockGlowsS2C.Entry(glow.pos().asLong(), BuiltInRegistries.BLOCK.getId(glow.block()),
            style.color(), style.flags(), remaining);
    }

    private static void send(ServerPlayer player, boolean reset, List<BlockGlowsS2C.Entry> adds, LongArrayList removes) {
        if (adds.isEmpty() && removes.isEmpty()) return;
        int from = 0;
        boolean first = true;
        do {
            int to = Math.min(adds.size(), from + BlockGlowsS2C.MAX_ENTRIES);
            List<BlockGlowsS2C.Entry> slice = adds.subList(from, to);
            long[] removed = first ? removes.toLongArray() : new long[0];
            ApoliNetwork.sendBlockGlows(player, new BlockGlowsS2C(reset && first, new ArrayList<>(slice), removed));
            first = false;
            from = to;
        } while (from < adds.size());
    }

    public static void load(MinecraftServer server) {
        long now = clock(server);
        for (ServerLevel level : server.getAllLevels()) {
            Dimension dimension = DIMENSIONS.computeIfAbsent(level.dimension(), key -> new Dimension());
            if (dimension.data == null) attach(level, dimension, now);
            if (dimension.idle()) DIMENSIONS.remove(level.dimension());
        }
    }

    private static void attach(ServerLevel level, Dimension dimension, long now) {
        BlockGlowSavedData data = BlockGlowSavedData.get(level);
        dimension.data = data;
        ListTag stored = data.takeLoaded();
        if (stored == null || stored.isEmpty()) return;
        Map<Tag, GlowBlockAction.Cfg> interned = new HashMap<>();
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            long expiresAt = entry.getLong("expires");
            if (expiresAt >= 0 && expiresAt <= now) continue;
            ResourceLocation blockId = ResourceLocation.tryParse(entry.getString("block"));
            if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) continue;
            CompoundTag specTag = entry.getCompound("spec");
            GlowBlockAction.Cfg spec = interned.get(specTag);
            if (spec == null) {
                spec = decodeSpec(specTag);
                if (spec == null) continue;
                interned.put(specTag, spec);
            }
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            Glow glow = new Glow(pos, BuiltInRegistries.BLOCK.get(blockId), spec, expiresAt);
            Glow previous = dimension.glows.put(pos.asLong(), glow);
            if (previous != null) dimension.release(previous.spec());
            dimension.retain(spec);
            if (expiresAt >= 0) {
                EXPIRY.computeIfAbsent(expiresAt, t -> new ArrayList<>(1)).add(new Key(level.dimension(), pos.asLong()));
            }
        }
    }

    static ListTag save(ResourceKey<Level> where) {
        ListTag list = new ListTag();
        Dimension dimension = DIMENSIONS.get(where);
        if (dimension == null || dimension.glows.isEmpty()) return list;
        Map<GlowBlockAction.Cfg, CompoundTag> encoded = new IdentityHashMap<>();
        for (Long2ObjectMap.Entry<Glow> entry : dimension.glows.long2ObjectEntrySet()) {
            Glow glow = entry.getValue();
            CompoundTag spec = encoded.computeIfAbsent(glow.spec(), BlockGlows::encodeSpec);
            if (spec.isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", glow.pos().asLong());
            tag.putString("block", BuiltInRegistries.BLOCK.getKey(glow.block()).toString());
            tag.putLong("expires", glow.expiresAt());
            tag.put("spec", spec);
            list.add(tag);
        }
        return list;
    }

    private static CompoundTag encodeSpec(GlowBlockAction.Cfg spec) {
        CompoundTag tag = new CompoundTag();
        BlockGlowStyle style = spec.style();
        tag.putFloat("red", style.red());
        tag.putFloat("green", style.green());
        tag.putFloat("blue", style.blue());
        tag.putBoolean("respect_bounding", style.respectBounding());
        tag.putString("mode", style.mode().getSerializedName());
        if (spec.entityCondition().isPresent()) {
            DataResult<Tag> condition = EntityCondition.CODEC.encodeStart(NbtOps.INSTANCE, spec.entityCondition().get());
            Optional<Tag> result = condition.result();
            if (result.isEmpty()) {
                if (!warnedEncode) {
                    warnedEncode = true;
                    Apoli.LOGGER.warn("[Apoli] A block glow's entity_condition could not be saved, so that glow ends when the "
                        + "server stops: {}", condition.error().map(error -> error.message()).orElse("unknown error"));
                }
                return new CompoundTag();
            }
            tag.put("entity_condition", result.get());
        }
        return tag;
    }

    private static @Nullable GlowBlockAction.Cfg decodeSpec(CompoundTag tag) {
        BlockGlowStyle.Mode mode = "glow".equals(tag.getString("mode")) ? BlockGlowStyle.Mode.GLOW : BlockGlowStyle.Mode.OUTLINE;
        BlockGlowStyle style = new BlockGlowStyle(tag.getFloat("red"), tag.getFloat("green"), tag.getFloat("blue"),
            tag.getBoolean("respect_bounding"), mode);
        Optional<EntityCondition> condition = Optional.empty();
        if (tag.contains("entity_condition")) {
            DataResult<EntityCondition> parsed = EntityCondition.CODEC.parse(NbtOps.INSTANCE, tag.get("entity_condition"));
            if (parsed.result().isEmpty()) {
                Apoli.LOGGER.warn("[Apoli] Dropped a saved block glow whose entity_condition no longer loads: {}",
                    parsed.error().map(error -> error.message()).orElse("unknown error"));
                return null;
            }
            condition = parsed.result();
        }
        return new GlowBlockAction.Cfg(style, condition, 0);
    }

    public static void clear() {
        DIMENSIONS.clear();
        VIEWERS.clear();
        EXPIRY.clear();
    }

    private static Dimension dimension(ServerLevel level) {
        Dimension dimension = DIMENSIONS.computeIfAbsent(level.dimension(), key -> new Dimension());
        if (dimension.data == null) attach(level, dimension, clock(level.getServer()));
        return dimension;
    }

    private static long clock(MinecraftServer server) {
        return server.overworld().getGameTime();
    }
}
