package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.entity.OfflinePlayers;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class EntitySetPower extends PowerType<EntitySetPower.Cfg> {
    public record Cfg(Optional<BiEntityAction> actionOnAdd, Optional<BiEntityAction> actionOnRemove, boolean persistent) {}

    public interface ClientView {
        boolean contains(Entity owner, ResourceLocation powerId, Entity member);

        int size(Entity owner, ResourceLocation powerId);
    }

    private record SavedSet(UUID owner, ResourceLocation set, List<UUID> members, Map<UUID, Long> expireAt) {}

    private static final Codec<List<SavedSet>> SAVED_SETS = RecordCodecBuilder.<SavedSet>create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("owner").forGetter(SavedSet::owner),
        ResourceLocation.CODEC.fieldOf("set").forGetter(SavedSet::set),
        UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(SavedSet::members),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("expire_at", Map.of()).forGetter(SavedSet::expireAt)
    ).apply(i, SavedSet::new)).listOf();

    private static final Map<UUID, Map<ResourceLocation, SetState>> BY_OWNER = new HashMap<>();
    private static final Map<UUID, Set<SetState>> MEMBERSHIPS = new HashMap<>();
    private static final Set<SetState> DIRTY = new LinkedHashSet<>();
    private static final Map<UUID, Boolean> PENDING_REMOVAL = new LinkedHashMap<>();

    private static @Nullable ClientView clientView;
    private static @Nullable Runnable persistentChanged;

    public static void setClientView(ClientView view) {
        clientView = view;
    }

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("action_on_add", BiEntityAction.CODEC).forGetter(Cfg::actionOnAdd),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("action_on_remove", BiEntityAction.CODEC).forGetter(Cfg::actionOnRemove),
            Codec.BOOL.optionalFieldOf("persistent", false).forGetter(Cfg::persistent)
        ).apply(i, Cfg::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        SetState state = state(owner.getUUID(), powerId);
        if (state == null || state.expireAt == null || state.expireAt.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = state.expireAt.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now < e.getValue()) continue;
            UUID uuid = e.getKey();
            it.remove();
            if (state.members.remove(uuid)) {
                unlink(uuid, state);
                DIRTY.add(state);
                changed(state);
                runBi(cfg.actionOnRemove, owner, uuid, level);
            }
        }
        dropIfEmpty(state);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        SetState state = state(owner.getUUID(), powerId);
        if (state == null) return;
        List<UUID> members = List.copyOf(state.members);
        changed(state);
        drop(state);
        DIRTY.add(state);
        if (cfg.actionOnRemove.isEmpty()) return;
        for (UUID uuid : members) {
            runBi(cfg.actionOnRemove, owner, uuid, level);
        }
    }

    public static @Nullable Cfg resolveCfg(ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return null;
        return loaded.config() instanceof Cfg c ? c : null;
    }

    public static boolean isPersistent(ResourceLocation powerId) {
        Cfg cfg = resolveCfg(powerId);
        return cfg != null && cfg.persistent;
    }

    public static boolean add(Entity owner, ResourceLocation powerId, Cfg cfg,
                              Entity target, OptionalInt timeLimit) {
        if (target.isRemoved()) return false;
        if (!(owner.level() instanceof ServerLevel level)) return false;
        SetState state = stateOrCreate(owner.getUUID(), powerId);
        UUID uuid = target.getUUID();
        boolean firstTime = state.members.add(uuid);
        if (timeLimit.isPresent()) {
            state.expiry().put(uuid, level.getGameTime() + timeLimit.getAsInt());
        }
        if (firstTime) {
            link(uuid, state);
            DIRTY.add(state);
        }
        if (firstTime || timeLimit.isPresent()) changed(state);
        if (firstTime) {
            cfg.actionOnAdd.ifPresent(a -> a.run(new BiEntityCtx(owner, target, level)));
        }
        return firstTime;
    }

    public static boolean remove(Entity owner, ResourceLocation powerId, Cfg cfg, Entity target) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        SetState state = state(owner.getUUID(), powerId);
        if (state == null) return false;
        UUID uuid = target.getUUID();
        boolean removed = state.members.remove(uuid);
        boolean timed = state.expireAt != null && state.expireAt.remove(uuid) != null;
        if (removed) {
            unlink(uuid, state);
            DIRTY.add(state);
        }
        if (removed || timed) changed(state);
        dropIfEmpty(state);
        if (removed) cfg.actionOnRemove.ifPresent(a -> a.run(new BiEntityCtx(owner, target, level)));
        return removed;
    }

    public static boolean contains(Entity owner, ResourceLocation powerId, Entity target) {
        if ((target.isRemoved() || !target.isAlive()) && !isPersistent(powerId)) return false;
        if (owner.level().isClientSide()) {
            ClientView view = clientView;
            return view != null && view.contains(owner, powerId, target);
        }
        SetState state = state(owner.getUUID(), powerId);
        return state != null && state.members.contains(target.getUUID());
    }

    public static int size(Entity owner, ResourceLocation powerId) {
        if (owner.level().isClientSide()) {
            ClientView view = clientView;
            return view == null ? 0 : view.size(owner, powerId);
        }
        SetState state = state(owner.getUUID(), powerId);
        return state == null ? 0 : state.members.size();
    }

    public static List<UUID> iterationOrder(Entity owner, ResourceLocation powerId, boolean reverse) {
        SetState state = state(owner.getUUID(), powerId);
        if (state == null || state.members.isEmpty()) return List.of();
        List<UUID> list = new ArrayList<>(state.members);
        if (reverse) Collections.reverse(list);
        return list;
    }

    public static List<UUID> ownersContaining(Entity member, ResourceLocation powerId, boolean reverse) {
        Set<SetState> states = MEMBERSHIPS.get(member.getUUID());
        if (states == null || states.isEmpty()) return List.of();
        List<UUID> owners = null;
        for (SetState state : states) {
            if (!state.powerId.equals(powerId)) continue;
            if (owners == null) owners = new ArrayList<>(2);
            owners.add(state.owner);
        }
        if (owners == null) return List.of();
        if (reverse) Collections.reverse(owners);
        return owners;
    }

    public static int membershipCount(Entity member, @Nullable ResourceLocation powerId) {
        Set<SetState> states = MEMBERSHIPS.get(member.getUUID());
        if (states == null) return 0;
        if (powerId == null) return states.size();
        int count = 0;
        for (SetState state : states) {
            if (state.powerId.equals(powerId)) count++;
        }
        return count;
    }

    public static void flushSync(MinecraftServer server) {
        if (DIRTY.isEmpty()) return;
        for (SetState state : DIRTY) {
            List<UUID> members = state.members.isEmpty() ? List.of() : List.copyOf(state.members);
            List<UUID> previous = state.lastSent;
            if (previous == null ? members.isEmpty() : previous.equals(members)) continue;
            state.lastSent = members.isEmpty() ? null : members;
            broadcast(server, state, members);
        }
        DIRTY.clear();
    }

    private static void broadcast(MinecraftServer server, SetState state, List<UUID> members) {
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(state.owner);
        if (ownerPlayer != null) {
            dev.overgrown.apoli.ApoliNetwork.sendEntitySets(ownerPlayer,
                new dev.overgrown.apoli.network.payload.SyncEntitySetsS2C(state.powerId, members));
        }
        Entity owner = ownerPlayer != null ? ownerPlayer : resolveEntity(server, state.owner);
        if (owner == null) return;
        dev.overgrown.apoli.ApoliNetwork.sendEntitySetToTrackers(owner,
            new dev.overgrown.apoli.network.payload.SyncTrackedEntitySetS2C(state.owner, state.powerId, members));
    }

    public static void syncAllTo(ServerPlayer player) {
        Map<ResourceLocation, SetState> owned = BY_OWNER.get(player.getUUID());
        if (owned == null) return;
        for (SetState state : owned.values()) {
            List<UUID> members = List.copyOf(state.members);
            dev.overgrown.apoli.ApoliNetwork.sendEntitySets(player,
                new dev.overgrown.apoli.network.payload.SyncEntitySetsS2C(state.powerId, members));
            state.lastSent = members.isEmpty() ? null : members;
        }
    }

    public static void syncTracking(Entity owner, ServerPlayer viewer, boolean tracking) {
        if (BY_OWNER.isEmpty() || owner == viewer) return;
        Map<ResourceLocation, SetState> owned = BY_OWNER.get(owner.getUUID());
        if (owned == null) return;
        for (SetState state : owned.values()) {
            if (state.members.isEmpty()) continue;
            List<UUID> members = tracking ? List.copyOf(state.members) : List.of();
            dev.overgrown.apoli.ApoliNetwork.sendTrackedEntitySet(viewer,
                new dev.overgrown.apoli.network.payload.SyncTrackedEntitySetS2C(state.owner, state.powerId, members));
        }
    }

    public static @Nullable Entity resolveEntity(MinecraftServer server, UUID uuid) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null && !e.isRemoved()) return e;
        }
        return null;
    }

    public static @Nullable Entity resolveEntity(MinecraftServer server, UUID uuid, boolean includeOffline) {
        Entity live = resolveEntity(server, uuid);
        if (live != null || !includeOffline || server == null) return live;
        return OfflinePlayers.get(server, uuid);
    }

    public static void onEntityGone(Entity entity) {
        PENDING_REMOVAL.put(entity.getUUID(), entity instanceof Player);
    }

    public static void onEntityConverted(UUID oldUuid, UUID newUuid) {
        if (oldUuid.equals(newUuid)) return;
        PENDING_REMOVAL.remove(oldUuid);
        Set<SetState> states = MEMBERSHIPS.remove(oldUuid);
        if (states != null) {
            for (SetState state : states) {
                if (state.members.remove(oldUuid)) {
                    state.members.add(newUuid);
                    DIRTY.add(state);
                }
                if (state.expireAt != null) {
                    Long expiry = state.expireAt.remove(oldUuid);
                    if (expiry != null) state.expireAt.put(newUuid, expiry);
                }
                link(newUuid, state);
                changed(state);
            }
        }
        Map<ResourceLocation, SetState> owned = BY_OWNER.remove(oldUuid);
        if (owned == null) return;
        Map<ResourceLocation, SetState> target = BY_OWNER.computeIfAbsent(newUuid, k -> new HashMap<>(4));
        for (SetState state : owned.values()) {
            state.owner = newUuid;
            target.putIfAbsent(state.powerId, state);
            DIRTY.add(state);
            changed(state);
        }
    }

    public static void flushPendingRemovals() {
        if (PENDING_REMOVAL.isEmpty()) return;
        for (Map.Entry<UUID, Boolean> gone : PENDING_REMOVAL.entrySet()) {
            UUID uuid = gone.getKey();
            Set<SetState> states = MEMBERSHIPS.get(uuid);
            if (states != null) {
                for (SetState state : List.copyOf(states)) {
                    if (state.persistent()) continue;
                    if (state.members.remove(uuid)) DIRTY.add(state);
                    if (state.expireAt != null) state.expireAt.remove(uuid);
                    unlink(uuid, state);
                    dropIfEmpty(state);
                }
            }
            if (!gone.getValue()) dropOwner(uuid);
        }
        PENDING_REMOVAL.clear();
    }

    public static void attachStorage(@Nullable Runnable onChange) {
        persistentChanged = onChange;
    }

    public static CompoundTag savePersistent(CompoundTag tag) {
        List<SavedSet> saved = new ArrayList<>();
        for (Map<ResourceLocation, SetState> owned : BY_OWNER.values()) {
            for (SetState state : owned.values()) {
                if (state.members.isEmpty() || !state.persistent()) continue;
                saved.add(new SavedSet(state.owner, state.powerId, List.copyOf(state.members),
                    state.expireAt == null ? Map.of() : Map.copyOf(state.expireAt)));
            }
        }
        SAVED_SETS.encodeStart(NbtOps.INSTANCE, saved)
            .resultOrPartial(error -> Apoli.LOGGER.error("[Apoli] Could not save persistent entity sets: {}", error))
            .ifPresent(sets -> tag.put("sets", sets));
        return tag;
    }

    public static void loadPersistent(CompoundTag tag) {
        Tag sets = tag.get("sets");
        if (sets == null) return;
        SAVED_SETS.parse(NbtOps.INSTANCE, sets)
            .resultOrPartial(error -> Apoli.LOGGER.error("[Apoli] Could not load persistent entity sets: {}", error))
            .ifPresent(list -> {
                for (SavedSet saved : list) restore(saved);
            });
    }

    private static void restore(SavedSet saved) {
        if (saved.members.isEmpty()) return;
        SetState state = stateOrCreate(saved.owner, saved.set);
        state.loadedPersistent = true;
        for (UUID member : saved.members) {
            if (state.members.add(member)) link(member, state);
        }
        for (Map.Entry<UUID, Long> expiry : saved.expireAt.entrySet()) {
            if (state.members.contains(expiry.getKey())) state.expiry().put(expiry.getKey(), expiry.getValue());
        }
    }

    public static void reset() {
        BY_OWNER.clear();
        MEMBERSHIPS.clear();
        DIRTY.clear();
        PENDING_REMOVAL.clear();
        persistentChanged = null;
    }

    private static @Nullable SetState state(UUID owner, ResourceLocation powerId) {
        Map<ResourceLocation, SetState> owned = BY_OWNER.get(owner);
        return owned == null ? null : owned.get(powerId);
    }

    private static SetState stateOrCreate(UUID owner, ResourceLocation powerId) {
        Map<ResourceLocation, SetState> owned = BY_OWNER.computeIfAbsent(owner, k -> new HashMap<>(4));
        SetState state = owned.get(powerId);
        if (state == null) {
            state = new SetState(owner, powerId);
            owned.put(powerId, state);
        }
        return state;
    }

    private static void dropIfEmpty(SetState state) {
        if (state.members.isEmpty()) drop(state);
    }

    private static void drop(SetState state) {
        Map<ResourceLocation, SetState> owned = BY_OWNER.get(state.owner);
        if (owned != null && owned.get(state.powerId) == state) {
            owned.remove(state.powerId);
            if (owned.isEmpty()) BY_OWNER.remove(state.owner);
        }
        for (UUID member : state.members) unlink(member, state);
        state.members.clear();
        state.expireAt = null;
    }

    private static void dropOwner(UUID owner) {
        Map<ResourceLocation, SetState> owned = BY_OWNER.get(owner);
        if (owned == null) return;
        for (SetState state : List.copyOf(owned.values())) {
            changed(state);
            drop(state);
        }
    }

    private static void changed(SetState state) {
        Runnable onChange = persistentChanged;
        if (onChange != null && state.persistent()) onChange.run();
    }

    private static void link(UUID member, SetState state) {
        MEMBERSHIPS.computeIfAbsent(member, k -> new LinkedHashSet<>(2)).add(state);
    }

    private static void unlink(UUID member, SetState state) {
        Set<SetState> states = MEMBERSHIPS.get(member);
        if (states == null) return;
        states.remove(state);
        if (states.isEmpty()) MEMBERSHIPS.remove(member);
    }

    private static void runBi(Optional<BiEntityAction> action, LivingEntity owner, UUID targetUuid, ServerLevel level) {
        if (action.isEmpty()) return;
        Entity target = resolveEntity(level.getServer(), targetUuid);
        if (target == null) return;
        action.get().run(new BiEntityCtx(owner, target, level));
    }

    private static final class SetState {
        UUID owner;
        final ResourceLocation powerId;
        final LinkedHashSet<UUID> members = new LinkedHashSet<>();
        @Nullable Map<UUID, Long> expireAt;
        @Nullable List<UUID> lastSent;
        boolean loadedPersistent;

        SetState(UUID owner, ResourceLocation powerId) {
            this.owner = owner;
            this.powerId = powerId;
        }

        Map<UUID, Long> expiry() {
            Map<UUID, Long> map = expireAt;
            if (map == null) {
                map = new HashMap<>(4);
                expireAt = map;
            }
            return map;
        }

        boolean persistent() {
            Cfg cfg = resolveCfg(powerId);
            return cfg != null ? cfg.persistent : loadedPersistent;
        }
    }
}
