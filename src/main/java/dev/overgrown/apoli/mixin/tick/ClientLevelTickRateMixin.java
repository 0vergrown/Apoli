package dev.overgrown.apoli.mixin.tick;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.overgrown.apoli.client.ClientTickRates;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
@OnlyIn(Dist.CLIENT)
public abstract class ClientLevelTickRateMixin {

    @Inject(method = "tickEntities()V", at = @At("HEAD"))
    private void apoli$beginClientTick(CallbackInfo ci) {
        ClientTickRates.beginClientTick(((ClientLevel) (Object) this).getGameTime());
    }

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$gateEntityTick(Entity entity, CallbackInfo ci) {
        if (ClientTickRates.idle()) return;
        if (!ClientTickRates.isGated(entity)) return;
        if (ClientTickRates.simulatedOnClient(entity) && ClientTickRates.shouldTick(entity)) return;
        ClientTickRates.keepRenderPose(entity);
        ClientTickRates.advanceLerp(entity);
        ClientTickRates.fadeHurt(entity);
        ci.cancel();
    }

    @WrapWithCondition(method = "tickPassenger(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private boolean apoli$gatePassengerTick(Entity passenger, Entity vehicle, Entity rider) {
        if (!ClientTickRates.holdsPassenger(passenger)) return true;
        vehicle.positionRider(passenger);
        return false;
    }

    @Inject(
        method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOldPosAndRot()V", shift = At.Shift.AFTER))
    private void apoli$followOnGateTick(Entity entity, CallbackInfo ci) {
        if (ClientTickRates.idle()) return;
        ClientTickRates.advanceLerp(entity);
    }
}
