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

    @Override
    public MapCodec<Cfg> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.optionalFieldOf("action_on_add").forGetter(Cfg::actionOnAdd),
            BiEntityAction.CODEC.optionalFieldOf("action_on_remove").forGetter(Cfg::actionOnRemove)
        ).apply(i, Cfg::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        State state = STATES.get(new StateKey(owner.getUUID(), powerId));
        if (state == null || state.expireAt.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = state.expireAt.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now < e.getValue()) continue;
            UUID uuid = e.getKey();
            it.remove();
            if (state.uuids.remove(uuid)) {
                cfg.actionOnRemove.ifPresent(a -> runBi(cfg.actionOnRemove, owner, uuid, level));
            }
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        LivingEntity owner = holder.owner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        State state = STATES.remove(new StateKey(owner.getUUID(), powerId));
        if (state == null) return;
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
        State state = STATES.computeIfAbsent(new StateKey(owner.getUUID(), powerId), k -> new State());
        UUID uuid = target.getUUID();
        boolean firstTime = state.uuids.add(uuid);
        if (timeLimit.isPresent()) {
            state.expireAt.put(uuid, level.getGameTime() + timeLimit.getAsInt());
        }
        if (firstTime) {
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
        for (State s : STATES.values()) {
            if (s.uuids.remove(oldUuid)) {
                s.uuids.add(newUuid);
            }
            Long expiry = s.expireAt.remove(oldUuid);
            if (expiry != null) s.expireAt.put(newUuid, expiry);
        }
    }

    public static void flushPendingRemovals() {
        if (PENDING_REMOVAL.isEmpty()) return;
        for (UUID uuid : PENDING_REMOVAL) {
            for (State s : STATES.values()) {
                s.uuids.remove(uuid);
                s.expireAt.remove(uuid);
            }
        }
        PENDING_REMOVAL.clear();
        Iterator<Map.Entry<StateKey, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            State s = it.next().getValue();
            if (s.uuids.isEmpty() && s.expireAt.isEmpty()) it.remove();
        }
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
