package dev.overgrown.apoli.compat.accessory.mixin.trinkets;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotReference;
import dev.overgrown.apoli.compat.accessory.backend.TrinketsBackend;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryEquipPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(TrinketSlot.class)
public interface TrinketSlotMixin {

    @ModifyReturnValue(method = "canInsert", at = @At("RETURN"))
    private static boolean apoli$preventEquip(boolean original, ItemStack stack, SlotReference slotRef, LivingEntity entity) {
        return original && !PreventAccessoryEquipPower.isPrevented(entity, TrinketsBackend.slotRef(slotRef), stack);
    }
}
