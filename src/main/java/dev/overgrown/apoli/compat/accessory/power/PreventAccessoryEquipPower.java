package dev.overgrown.apoli.compat.accessory.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public final class PreventAccessoryEquipPower extends BasePreventAccessoryPower {
    public static final ResourceLocation CANONICAL = Apoli.id("prevent_accessory_equip");

    @Override
    public MapCodec<Config> configCodec() {
        return baseCodec();
    }

    public static boolean isPrevented(LivingEntity entity, AccessorySlotRef ref, ItemStack stack) {
        return isPrevented(entity, CANONICAL, ref, stack);
    }
}
