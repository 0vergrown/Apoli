package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.client.ClientTickRates;
import dev.overgrown.apoli.tick.TickRates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class LocalPlayerTickRateMixin {

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void apoli$gateLocalPlayer(CallbackInfo ci) {
        if (ClientTickRates.idle()) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        int rate = ClientTickRates.rateOf(self);
        if (rate < 0) return;
        if (rate > 0 && TickRates.gate(rate, ClientTickRates.baseRate(), self.level().getGameTime())) return;
        ClientTickRates.keepViewBob(self);
        ci.cancel();
    }
}
