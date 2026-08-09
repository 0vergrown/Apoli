package dev.overgrown.apoli.compat.skinlayers;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
