package dev.overgrown.apoli.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ItemPickupHandler;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Unique
    private ItemPickupHandler.Pending apoli$pickup;

    @ModifyArg(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private ItemStack apoli$beforePickup(ItemStack stack, @Local(argsOnly = true) Player player) {
        this.apoli$pickup = null;
        if (!ItemPickupHandler.watches(player)) return stack;
        ItemEntity self = (ItemEntity) (Object) this;
        if (ItemPickupHandler.prevents(self, player)) return ItemStack.EMPTY;
        this.apoli$pickup = ItemPickupHandler.before(self, player, ItemPickupHandler.hasRoom(player, stack));
        return self.getItem();
    }

    @ModifyExpressionValue(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean apoli$afterPickup(boolean added, @Local(argsOnly = true) Player player) {
        ItemPickupHandler.Pending pending = this.apoli$pickup;
        if (pending == null) return added;
        this.apoli$pickup = null;
        if (added) ItemPickupHandler.after(pending, player);
        return added;
    }
}
