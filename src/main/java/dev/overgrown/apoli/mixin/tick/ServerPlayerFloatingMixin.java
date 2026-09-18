package dev.overgrown.apoli.mixin.tick;

import dev.overgrown.apoli.tick.TickRates;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayerFloatingMixin {

    @Shadow private boolean clientIsFloating;
    @Shadow private boolean clientVehicleIsFloating;
    @Shadow public ServerPlayer player;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void apoli$forgiveGatedFloat(CallbackInfo ci) {
        if (TickRates.idle()) return;
        if (TickRates.shouldTickEntity(this.player)) return;
        this.clientIsFloating = false;
        this.clientVehicleIsFloating = false;
    }
}
