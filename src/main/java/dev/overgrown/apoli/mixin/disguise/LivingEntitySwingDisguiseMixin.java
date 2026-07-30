package dev.overgrown.apoli.mixin.disguise;

import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntitySwingDisguiseMixin {

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("TAIL"))
    private void apoli$swingDisguise(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide) return;
        ClientDisguiseManager.onSwing(self, hand);
    }
}
