package dev.overgrown.apoli.compat.skinlayers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class SkinLayersState {

    private static LivingEntity current;

    private SkinLayersState() {}

    public static void begin(LivingEntity entity) {
        current = entity;
    }

    public static void end() {
        current = null;
    }

    @Nullable
    public static LivingEntity current() {
        return current;
    }
}
