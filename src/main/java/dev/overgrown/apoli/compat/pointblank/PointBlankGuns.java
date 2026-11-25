package dev.overgrown.apoli.compat.pointblank;

import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PointBlankGuns {
    private PointBlankGuns() {}

    public static @Nullable ResourceLocation gunId(ItemStack stack) {
        return stack.getItem() instanceof GunItem ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : null;
    }

    public static ResourceLocation hurtingGun(Item self, @Nullable ItemStack gunStack) {
        Item item = gunStack != null && !gunStack.isEmpty() ? gunStack.getItem() : self;
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static boolean isAiming(LivingEntity entity) {
        ItemStack held = entity.getMainHandItem();
        return held.getItem() instanceof GunItem && GunItem.isAiming(held);
    }
}
