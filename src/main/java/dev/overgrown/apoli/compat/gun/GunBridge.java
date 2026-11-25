package dev.overgrown.apoli.compat.gun;

import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.compat.pointblank.PointBlankGuns;
import dev.overgrown.apoli.compat.tacz.TaczGuns;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class GunBridge {
    private GunBridge() {}

    public static @Nullable ResourceLocation gunId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (ModCompat.POINT_BLANK) {
            ResourceLocation id = PointBlankGuns.gunId(stack);
            if (id != null) return id;
        }
        if (ModCompat.TACZ) return TaczGuns.gunId(stack);
        return null;
    }

    public static boolean isAiming(LivingEntity entity) {
        if (ModCompat.POINT_BLANK && PointBlankGuns.isAiming(entity)) return true;
        return ModCompat.TACZ && TaczGuns.isAiming(entity);
    }
}
