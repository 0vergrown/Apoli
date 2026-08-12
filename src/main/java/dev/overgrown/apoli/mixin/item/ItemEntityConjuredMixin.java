package dev.overgrown.apoli.mixin.item;

import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityConjuredMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void apoli$discardConjured(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide() && ConjuredItems.isConjured(self.getItem())) {
            self.discard();
            ci.cancel();
        }
    }
}
