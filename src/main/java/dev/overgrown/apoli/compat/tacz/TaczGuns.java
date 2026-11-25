package dev.overgrown.apoli.compat.tacz;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class TaczGuns {
    private TaczGuns() {}

    public static @Nullable ResourceLocation gunId(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        return gun == null ? null : gun.getGunId(stack);
    }

    public static boolean isAiming(LivingEntity entity) {
        return IGun.getIGunOrNull(entity.getMainHandItem()) != null && IGunOperator.fromLivingEntity(entity).getSynIsAiming();
    }
}
