package dev.overgrown.apoli.client.disguise;

import dev.overgrown.apoli.entity.disguise.DisguiseData;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientDisguiseManager {
    private ClientDisguiseManager() {}

    private static final Map<Integer, DisguiseData> DISGUISES = new ConcurrentHashMap<>();
    private static final Map<Integer, Entity> DUMMIES = new ConcurrentHashMap<>();

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
        } else {
            createDummy(netId, data);
        }
    }

    public static void remove(int netId) {
        DISGUISES.remove(netId);
        DUMMIES.remove(netId);
    }

    public static void clear() {
        DISGUISES.clear();
        DUMMIES.clear();
    }

    @Nullable
    public static DisguiseData get(int netId) {
        return DISGUISES.get(netId);
    }

    @Nullable
    public static Entity syncedDummy(int netId, Entity actor) {
        Entity dummy = DUMMIES.get(netId);
        if (dummy == null) return null;

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

        if (dummy instanceof LivingEntity dummyLiving && actor instanceof LivingEntity actorLiving) {
            dummyLiving.yBodyRot = actorLiving.yBodyRot;
            dummyLiving.yBodyRotO = actorLiving.yBodyRotO;
            dummyLiving.yHeadRot = actorLiving.yHeadRot;
            dummyLiving.yHeadRotO = actorLiving.yHeadRotO;
            dummyLiving.hurtTime = actorLiving.hurtTime;
            dummyLiving.deathTime = actorLiving.deathTime;
            dummyLiving.attackAnim = actorLiving.attackAnim;
            dummyLiving.oAttackAnim = actorLiving.oAttackAnim;
        }
        return dummy;
    }

    private static void createDummy(int netId, DisguiseData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(data.entityTypeId());
        if (type == null) return;
        try {
            Entity dummy = type.create(mc.level);
            if (dummy == null) return;
            dummy.setId(-netId - 1);
            data.nbt().ifPresent(nbt -> {
                CompoundTag merged = dummy.saveWithoutId(new CompoundTag());
                merged.merge(nbt);
                dummy.load(merged);
            });
            DUMMIES.put(netId, dummy);
        } catch (Exception ignored) {
        }
    }
}
