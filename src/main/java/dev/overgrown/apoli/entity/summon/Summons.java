package dev.overgrown.apoli.entity.summon;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.entity.disguise.DisguiseManager;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.mixin.summon.ChunkMapEntityMapAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class Summons {
    private Summons() {}

    public static final ResourceLocation POWER_SOURCE = Apoli.id("summon");

    private static final double MOVED_SQR = 1.0E-4;

    public static void resendIfMoved(Entity summon, Vec3 spawnedAt) {
        if (summon.isRemoved() || !(summon.level() instanceof ServerLevel level)) return;
        if (summon.position().distanceToSqr(spawnedAt) < MOVED_SQR) return;
        ServerChunkCache chunks = level.getChunkSource();
        if (!((ChunkMapEntityMapAccessor) chunks.chunkMap).apoli$entityMap().containsKey(summon.getId())) return;
        chunks.removeEntity(summon);
        chunks.addEntity(summon);
    }

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
