package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotConjuredMixin {

    @Shadow @Final public Container container;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void apoli$lockConjured(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (ConjuredItems.isLocked(((Slot) (Object) this).getItem())) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true)
    private ItemStack apoli$deleteConjuredOutsideInventory(ItemStack stack) {
        if (this.container instanceof Inventory || !ConjuredItems.isConjured(stack)) {
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
