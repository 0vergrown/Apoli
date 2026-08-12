package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryDropLockedMixin {

    @Inject(method = "removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), cancellable = true)
    private void apoli$keepLockedConjured(boolean removeStack, CallbackInfoReturnable<ItemStack> cir) {
        Inventory self = (Inventory) (Object) this;
        if (ConjuredItems.isLocked(self.getSelected())) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
