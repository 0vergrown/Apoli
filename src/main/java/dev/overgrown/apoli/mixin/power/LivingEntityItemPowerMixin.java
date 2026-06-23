package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.item.ItemPowerHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityItemPowerMixin {

    @Unique private boolean apoli$itemPowersInit = false;
    @Unique private boolean apoli$hadItemPowers = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void apoli$reconcileItemPowers(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        if (!apoli$itemPowersInit) {
            apoli$itemPowersInit = true;
            apoli$hadItemPowers = ItemPowerHandler.reconcile(self);
            return;
        }

        if (!apoli$hadItemPowers && !ItemPowerHandler.anyEquippedPowers(self)) return;
        apoli$hadItemPowers = ItemPowerHandler.reconcile(self);
    }
}
