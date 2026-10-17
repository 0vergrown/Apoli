package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.power.builtin.NightVisionPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class LocalNightVision {

    private static int cachedId = Integer.MIN_VALUE;
    private static int cachedTick = Integer.MIN_VALUE;
    private static float cachedStrength;

    private LocalNightVision() {}

    public static float strength(@Nullable LivingEntity entity) {
        if (entity == null) return 0f;
        if (entity.getId() == cachedId && entity.tickCount == cachedTick) return cachedStrength;
        float strength = NightVisionPower.strengthFor(entity);
        cachedId = entity.getId();
        cachedTick = entity.tickCount;
        cachedStrength = strength;
        return strength;
    }
}
