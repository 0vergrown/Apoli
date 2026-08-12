package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.power.builtin.ItemOnItemHandler;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackItemOnItemMixin {
    @Inject(
        method = "overrideOtherStackedOnMe(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/SlotAccess;)Z",
        at = @At("RETURN"),
        cancellable = true
    )
    private void apoli$itemOnItem(ItemStack carried, Slot slot, ClickAction clickAction, Player player,
                                  SlotAccess cursor, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if (ItemOnItemHandler.handle(player, slot, cursor, clickAction)) cir.setReturnValue(true);
    }
}
