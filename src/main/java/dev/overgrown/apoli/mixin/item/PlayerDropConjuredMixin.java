package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDropConjuredMixin {

    @Inject(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true)
    private void apoli$swallowConjuredDrop(ItemStack stack, boolean dropAround, boolean includeThrowerName,
                                           CallbackInfoReturnable<ItemEntity> cir) {
        if (ConjuredItems.isConjured(stack)) {
            cir.setReturnValue(null);
        }
    }
}
