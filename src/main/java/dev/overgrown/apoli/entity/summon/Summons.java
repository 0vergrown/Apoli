package dev.overgrown.apoli.entity.summon;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import dev.overgrown.apoli.keybind.HeldKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class Summons {
    private Summons() {}

    public static final ResourceLocation POWER_SOURCE = Apoli.id("summon");

    public static void grantPowers(Entity summon, List<ResourceLocation> powers) {
        if (powers.isEmpty()) return;
        PowerContainer holder = PowerContainerAttachment.getOrCreate(summon);
        if (holder == null) return;
        for (ResourceLocation power : powers) {
            holder.addPower(power, POWER_SOURCE);
        }
    }

    public static void expire(LivingEntity summon) {
        if (summon.isRemoved()) return;
        if (summon.level() instanceof ServerLevel && !summon.isDeadOrDying()) {
            summon.hurt(summon.damageSources().genericKill(), Float.MAX_VALUE);
        }
        if (!summon.isRemoved()) summon.discard();
    }

    public static void onRemoved(Entity summon) {
        if (summon.level().isClientSide) return;
        DisguiseManager.remove(summon);
        HeldKeys.clearServer(summon.getUUID());
    }
}
