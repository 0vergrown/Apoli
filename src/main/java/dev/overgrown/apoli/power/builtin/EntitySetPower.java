package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class EntitySetPower extends PowerType<EntitySetPower.Cfg> {
    public record Cfg(Optional<BiEntityAction> actionOnAdd, Optional<BiEntityAction> actionOnRemove) {}

    private static final Map<StateKey, State> STATES = new HashMap<>();
    private static final Map<UUID, Set<StateKey>> MEMBERSHIPS = new HashMap<>();
    private static final Map<StateKey, List<UUID>> LAST_SENT = new HashMap<>();

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("action_on_add", BiEntityAction.CODEC).forGetter(Cfg::actionOnAdd),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("action_on_remove", BiEntityAction.CODEC).forGetter(Cfg::actionOnRemove)
        ).apply(i, Cfg::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        StateKey key = new StateKey(owner.getUUID(), powerId);
        State state = STATES.get(key);
        if (state == null || state.expireAt.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = state.expireAt.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now < e.getValue()) continue;
            UUID uuid = e.getKey();
            it.remove();
            if (state.uuids.remove(uuid)) {
                unlink(uuid, key);
                runBi(cfg.actionOnRemove, owner, uuid, level);
            }
        }
        if (state.uuids.isEmpty() && state.expireAt.isEmpty()) STATES.remove(key);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        LivingEntity owner = holder.owner();
        if (owner == null || !(owner.level() instanceof ServerLevel level)) return;
        StateKey key = new StateKey(owner.getUUID(), powerId);
        State state = STATES.remove(key);
        if (state == null) return;
        for (UUID uuid : state.uuids) unlink(uuid, key);
        if (cfg.actionOnRemove.isEmpty()) return;
        for (UUID uuid : state.uuids) {
            runBi(cfg.actionOnRemove, owner, uuid, level);
        }
    }

    public static @Nullable Cfg resolveCfg(ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return null;
        return loaded.config() instanceof Cfg c ? c : null;
    }

    public static boolean add(Entity owner, ResourceLocation powerId, Cfg cfg,
                              Entity target, OptionalInt timeLimit) {
        if (target.isRemoved()) return false;
        if (!(owner.level() instanceof ServerLevel level)) return false;
        StateKey key = new StateKey(owner.getUUID(), powerId);
        State state = STATES.computeIfAbsent(key, k -> new State());
        UUID uuid = target.getUUID();
        boolean firstTime = state.uuids.add(uuid);
        if (timeLimit.isPresent()) {
            state.expireAt.put(uuid, level.getGameTime() + timeLimit.getAsInt());
        }
        if (firstTime) {
            link(uuid, key);
            cfg.actionOnAdd.ifPresent(a -> a.run(new BiEntityCtx(owner, target, level)));
        }
        return firstTime;
    }

    public static boolean remove(Entity owner, ResourceLocation powerId, Cfg cfg, Entity target) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        StateKey key = new StateKey(owner.getUUID(), powerId);
        State state = STATES.get(key);
        if (state == null) return false;
        UUID uuid = target.getUUID();
        boolean removed = state.uuids.remove(uuid);
        state.expireAt.remove(uuid);
        if (removed) unlink(uuid, key);
        if (state.uuids.isEmpty() && state.expireAt.isEmpty()) STATES.remove(key);
        if (removed) cfg.actionOnRemove.ifPresent(a -> a.run(new BiEntityCtx(owner, target, level)));
        return removed;
    }

    public static boolean contains(Entity owner, ResourceLocation powerId, Entity target) {
        if (target.isRemoved() || !target.isAlive()) return false;
        State state = STATES.get(new StateKey(owner.getUUID(), powerId));
        return state != null && state.uuids.contains(target.getUUID());
    }

    public static int size(Entity owner, ResourceLocation powerId) {
        State state = STATES.get(new StateKey(owner.getUUID(), powerId));
        return state == null ? 0 : state.uuids.size();
    }

    public static List<UUID> iterationOrder(Entity owner, ResourceLocation powerId, boolean reverse) {
        State state = STATES.get(new StateKey(owner.getUUID(), powerId));
        if (state == null || state.uuids.isEmpty()) return List.of();
        List<UUID> list = new ArrayList<>(state.uuids);
        if (reverse) Collections.reverse(list);
        return list;
    }

    public static List<UUID> ownersContaining(Entity member, ResourceLocation powerId, boolean reverse) {
        Set<StateKey> keys = MEMBERSHIPS.get(member.getUUID());
        if (keys == null || keys.isEmpty()) return List.of();
        List<UUID> owners = null;
        for (StateKey key : keys) {
            if (!key.powerId.equals(powerId)) continue;
            if (owners == null) owners = new ArrayList<>(2);
            owners.add(key.owner);
        }
        if (owners == null) return List.of();
        if (reverse) Collections.reverse(owners);
        return owners;
    }

    public static void flushSync(MinecraftServer server) {
        for (Map.Entry<StateKey, State> entry : STATES.entrySet()) {
            StateKey key = entry.getKey();
            List<UUID> members = List.copyOf(entry.getValue().uuids);
            if (members.equals(LAST_SENT.get(key))) continue;
            if (!send(server, key, members)) continue;
            LAST_SENT.put(key, members);
        }
        if (LAST_SENT.isEmpty()) return;
        for (java.util.Iterator<Map.Entry<StateKey, List<UUID>>> it = LAST_SENT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<StateKey, List<UUID>> sent = it.next();
            if (STATES.containsKey(sent.getKey())) continue;
            send(server, sent.getKey(), List.of());
            it.remove();
        }
    }

    private static boolean send(MinecraftServer server, StateKey key, List<UUID> members) {
        net.minecraft.server.level.ServerPlayer owner = server.getPlayerList().getPlayer(key.owner);
        if (owner == null) return false;
        dev.overgrown.apoli.ApoliNetwork.sendEntitySets(owner,
            new dev.overgrown.apoli.network.payload.SyncEntitySetsS2C(key.powerId, members));
        return true;
    }

    public static void syncAllTo(net.minecraft.server.level.ServerPlayer player) {
        UUID uuid = player.getUUID();
        for (Map.Entry<StateKey, State> entry : STATES.entrySet()) {
            if (!entry.getKey().owner.equals(uuid)) continue;
            List<UUID> members = List.copyOf(entry.getValue().uuids);
            dev.overgrown.apoli.ApoliNetwork.sendEntitySets(player,
                new dev.overgrown.apoli.network.payload.SyncEntitySetsS2C(entry.getKey().powerId, members));
            LAST_SENT.put(entry.getKey(), members);
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

    private static final Set<UUID> PENDING_REMOVAL = new LinkedHashSet<>();

    public static void onEntityGone(UUID uuid) {
        PENDING_REMOVAL.add(uuid);
    }

    public static void onEntityConverted(UUID oldUuid, UUID newUuid) {
        if (oldUuid.equals(newUuid)) return;
        PENDING_REMOVAL.remove(oldUuid);
        Set<StateKey> keys = MEMBERSHIPS.remove(oldUuid);
        if (keys == null) return;
        for (StateKey key : keys) {
            State s = STATES.get(key);
            if (s == null) continue;
            if (s.uuids.remove(oldUuid)) s.uuids.add(newUuid);
            Long expiry = s.expireAt.remove(oldUuid);
            if (expiry != null) s.expireAt.put(newUuid, expiry);
            link(newUuid, key);
        }
    }

    public static void flushPendingRemovals() {
        if (PENDING_REMOVAL.isEmpty()) return;
        for (UUID uuid : PENDING_REMOVAL) {
            Set<StateKey> keys = MEMBERSHIPS.remove(uuid);
            if (keys == null) continue;
            for (StateKey key : keys) {
                State s = STATES.get(key);
                if (s == null) continue;
                s.uuids.remove(uuid);
                s.expireAt.remove(uuid);
                if (s.uuids.isEmpty() && s.expireAt.isEmpty()) STATES.remove(key);
            }
        }
        PENDING_REMOVAL.clear();
    }

    private static void link(UUID member, StateKey key) {
        MEMBERSHIPS.computeIfAbsent(member, k -> new LinkedHashSet<>(2)).add(key);
    }

    private static void unlink(UUID member, StateKey key) {
        Set<StateKey> keys = MEMBERSHIPS.get(member);
        if (keys == null) return;
        keys.remove(key);
        if (keys.isEmpty()) MEMBERSHIPS.remove(member);
    }

    private static void runBi(Optional<BiEntityAction> action, LivingEntity owner, UUID targetUuid, ServerLevel level) {
        if (action.isEmpty()) return;
        Entity target = resolveEntity(level.getServer(), targetUuid);
        if (target == null) return;
        action.get().run(new BiEntityCtx(owner, target, level));
    }

    private record StateKey(UUID owner, ResourceLocation powerId) {}

    private static final class State {
        final Set<UUID> uuids = new LinkedHashSet<>();
        final Map<UUID, Long> expireAt = new HashMap<>();
    }
}
