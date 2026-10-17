package dev.overgrown.apoli.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public abstract class ItemEntityConjuredMixin {

    @ModifyExpressionValue(method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean apoli$discardConjured(boolean empty) {
        if (empty) return true;
        ItemEntity self = (ItemEntity) (Object) this;
        return !self.level().isClientSide() && ConjuredItems.isConjured(self.getItem());
    }
}
