package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.power.builtin.DisableSlotPower;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryDisableSlotMixin {

    @Shadow @Final public Player player;

    @Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
    private void apoli$skipDisabledFreeSlot(CallbackInfoReturnable<Integer> cir) {
        int found = cir.getReturnValue();
        if (found < 0 || !DisableSlotPower.has(this.player)) return;
        if (!DisableSlotPower.disabled(this.player, found, ItemStack.EMPTY)) return;

        Inventory self = (Inventory) (Object) this;
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            if (!self.getItem(index).isEmpty()) continue;
            if (DisableSlotPower.disabled(this.player, index, ItemStack.EMPTY)) continue;
            cir.setReturnValue(index);
            return;
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
    private void apoli$skipDisabledPartialSlot(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int found = cir.getReturnValue();
        if (found < 0 || !DisableSlotPower.has(this.player)) return;
        if (DisableSlotPower.disabled(this.player, found, stack)) cir.setReturnValue(-1);
    }
}
