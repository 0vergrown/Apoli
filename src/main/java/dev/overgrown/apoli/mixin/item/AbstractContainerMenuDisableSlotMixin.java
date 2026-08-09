package dev.overgrown.apoli.mixin.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.builtin.DisableSlotPower;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuDisableSlotMixin {

    private static final String DO_CLICK =
        "doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V";
    private static final String MOVE_ITEM_STACK_TO =
        "moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z";

    @WrapOperation(method = {DO_CLICK, MOVE_ITEM_STACK_TO}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean apoli$refuseDisabledSlot(Slot slot, ItemStack stack, Operation<Boolean> original) {
        if (!original.call(slot, stack)) return false;
        if (!(slot.container instanceof Inventory inventory)) return true;
        if (!DisableSlotPower.has(inventory.player)) return true;
        return !DisableSlotPower.disabled(inventory.player, slot.getContainerSlot(), stack);
    }
}
