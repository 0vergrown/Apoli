package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.ApoliIds;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallFlyingMixin {
    @Unique
    private boolean apoli$wasFallFlying;

    @Inject(method = "updateFallFlying", at = @At("HEAD"))
    private void apoli$captureFallFlying(CallbackInfo ci) {
        apoli$wasFallFlying = ((LivingEntity) (Object) this).isFallFlying();
    }

    @Inject(method = "updateFallFlying", at = @At("TAIL"))
    private void apoli$sustainElytraFlight(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!apoli$wasFallFlying || self.isFallFlying()) return;
        if (self.onGround() || self.isPassenger() || self.hasEffect(MobEffects.LEVITATION)) return;
        if (self instanceof Player player && PowerLookup.hasActive(self, ApoliIds.ELYTRA_FLIGHT)) {
            player.startFallFlying();
        }
    }
}
