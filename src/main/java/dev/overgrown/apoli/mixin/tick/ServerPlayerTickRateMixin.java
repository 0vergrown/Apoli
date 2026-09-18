package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.tick.TickRates;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTickRateMixin {

    @Inject(method = "doTick()V", at = @At("HEAD"), cancellable = true)
    private void apoli$gatePlayerTick(CallbackInfo ci) {
        if (TickRates.idle()) return;
        if (!TickRates.shouldTickEntity((ServerPlayer) (Object) this)) ci.cancel();
    }
}
