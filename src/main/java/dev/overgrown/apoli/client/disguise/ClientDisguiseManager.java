package dev.overgrown.apoli.client.disguise;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.entity.disguise.DisguiseData;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientDisguiseManager {
    private ClientDisguiseManager() {}

    private static final byte ATTACK_EVENT = 4;

    private static final Map<Integer, DisguiseData> DISGUISES = new ConcurrentHashMap<>();
    private static final Map<Integer, Entity> DUMMIES = new ConcurrentHashMap<>();
    private static final Set<Integer> UNTICKABLE = ConcurrentHashMap.newKeySet();

    @Nullable
    private static Entity renderActor;

    public static void install() {
        DisguiseManager.setClientView(new DisguiseManager.ClientView() {
            @Override
            public boolean isDisguised(int netId) {
                return DISGUISES.containsKey(netId);
            }

            @Override
            public boolean isDisguisedAs(int actorNetId, Entity target) {
                return DisguiseManager.matches(DISGUISES.get(actorNetId), target);
            }
        });
    }

    public static void apply(int netId, DisguiseData data) {
        DISGUISES.put(netId, data);
        if (data.isPlayerDisguise()) {
            DUMMIES.remove(netId);
            UNTICKABLE.remove(netId);
        } else {
            createDummy(netId, data);
        }
    }

    public static void remove(int netId) {
        DISGUISES.remove(netId);
        DUMMIES.remove(netId);
        UNTICKABLE.remove(netId);
    }

    public static void clear() {
        DISGUISES.clear();
        DUMMIES.clear();
        UNTICKABLE.clear();
        renderActor = null;
    }

    @Nullable
    public static DisguiseData get(int netId) {
        return DISGUISES.get(netId);
    }

    @Nullable
    public static PlayerSkin playerSkin(int netId) {
        DisguiseData data = DISGUISES.get(netId);
        if (data == null || !data.isPlayerDisguise() || data.playerUuid().isEmpty()) return null;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return null;
        PlayerInfo info = connection.getPlayerInfo(data.playerUuid().get());
        return info == null ? null : info.getSkin();
    }

    public static void tick(Minecraft mc) {
        if (DUMMIES.isEmpty()) return;
        Level level = mc.level;
        if (level == null) {
            DUMMIES.clear();
            UNTICKABLE.clear();
            return;
        }
        if (mc.isPaused()) return;

        for (Iterator<Map.Entry<Integer, Entity>> it = DUMMIES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Entity> entry = it.next();
            Integer netId = entry.getKey();
            DisguiseData data = DISGUISES.get(netId);
            if (data == null || data.isPlayerDisguise()) {
                it.remove();
                UNTICKABLE.remove(netId);
                continue;
            }
            Entity dummy = entry.getValue();
            if (dummy.level() != level) {
                it.remove();
                UNTICKABLE.remove(netId);
                continue;
            }
            Entity actor = level.getEntity(netId);
            if (actor == null) continue;

            mirrorPose(actor, dummy);
            mirrorState(actor, dummy);
            if (!(dummy instanceof LivingEntity) || UNTICKABLE.contains(netId)) continue;
            try {
                dummy.tick();
            } catch (Throwable t) {
                UNTICKABLE.add(netId);
                Apoli.LOGGER.warn("[Apoli] Disguise dummy {} threw while ticking; its animations are disabled", data.entityTypeId(), t);
            }
            if (dummy.isRemoved()) UNTICKABLE.add(netId);
        }
    }

    public static void onSwing(Entity actor, InteractionHand hand) {
        Entity dummy = dummyFor(actor);
        if (dummy == null) return;
        dummy.tickCount = actor.tickCount;
        if (dummy instanceof LivingEntity living) living.swing(hand);
        fireEvent(dummy, ATTACK_EVENT);
    }

    public static void onEntityEvent(Entity actor, byte eventId) {
        Entity dummy = dummyFor(actor);
        if (dummy == null) return;
        dummy.tickCount = actor.tickCount;
        fireEvent(dummy, eventId);
    }

    @Nullable
    private static Entity dummyFor(Entity actor) {
        if (DUMMIES.isEmpty()) return null;
        Entity dummy = DUMMIES.get(actor.getId());
        if (dummy == null) return null;
        return dummy.level() == actor.level() ? dummy : null;
    }

    private static void fireEvent(Entity dummy, byte eventId) {
        try {
            dummy.handleEntityEvent(eventId);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    public static Entity syncedDummy(int netId, Entity actor) {
        Entity dummy = DUMMIES.get(netId);
        if (dummy == null || dummy.level() != actor.level()) {
            DisguiseData data = DISGUISES.get(netId);
            if (data == null || data.isPlayerDisguise()) return null;
            dummy = createDummy(netId, data);
            if (dummy == null) return null;
            mirrorState(actor, dummy);
        }
        mirrorPose(actor, dummy);
        return dummy;
    }

    @Nullable
    public static Entity beginDisguiseRender(Entity actor) {
        Entity previous = renderActor;
        renderActor = actor;
        return previous;
    }

    public static void endDisguiseRender(@Nullable Entity previous) {
        renderActor = previous;
    }

    public static Entity powerSource(Entity rendered) {
        Entity actor = renderActor;
        return actor != null && rendered.getId() == dummyId(actor.getId()) ? actor : rendered;
    }

    private static int dummyId(int netId) {
        return -netId - 1;
    }

    private static void mirrorPose(Entity actor, Entity dummy) {
        dummy.setPos(actor.getX(), actor.getY(), actor.getZ());
        dummy.xo = actor.xo;
        dummy.yo = actor.yo;
        dummy.zo = actor.zo;
        dummy.xOld = actor.xOld;
        dummy.yOld = actor.yOld;
        dummy.zOld = actor.zOld;
        dummy.setYRot(actor.getYRot());
        dummy.setXRot(actor.getXRot());
        dummy.yRotO = actor.yRotO;
        dummy.xRotO = actor.xRotO;
        dummy.tickCount = actor.tickCount;
        dummy.setRemainingFireTicks(actor.isOnFire() ? 1 : 0);

        if (dummy instanceof LivingEntity dummyLiving && actor instanceof LivingEntity actorLiving) {
            dummyLiving.yBodyRot = actorLiving.yBodyRot;
            dummyLiving.yBodyRotO = actorLiving.yBodyRotO;
            dummyLiving.yHeadRot = actorLiving.yHeadRot;
            dummyLiving.yHeadRotO = actorLiving.yHeadRotO;
            dummyLiving.hurtTime = actorLiving.hurtTime;
            dummyLiving.deathTime = actorLiving.deathTime;
        }
    }

    private static void mirrorState(Entity actor, Entity dummy) {
        dummy.setDeltaMovement(Vec3.ZERO);
        dummy.setOnGround(actor.onGround());
        dummy.setPose(actor.getPose());
        dummy.setSprinting(actor.isSprinting());
        dummy.setShiftKeyDown(actor.isShiftKeyDown());
    }

    @Nullable
    private static Entity createDummy(int netId, DisguiseData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(data.entityTypeId());
        if (type == null) return null;
        try {
            Entity dummy = type.create(mc.level);
            if (dummy == null) return null;
            data.nbt().ifPresent(nbt -> {
                CompoundTag merged = dummy.saveWithoutId(new CompoundTag());
                merged.merge(nbt);
                dummy.load(merged);
            });
            dummy.setId(dummyId(netId));
            dummy.setSilent(true);
            dummy.setNoGravity(true);
            dummy.noPhysics = true;
            DUMMIES.put(netId, dummy);
            UNTICKABLE.remove(netId);
            return dummy;
        } catch (Exception ignored) {
            return null;
        }
    }
}
