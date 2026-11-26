package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.builtin.InstantRespawnPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerInstantRespawnMixin {
    // Runs before the death packet goes out, so the client already has this player's setting when it decides
    // whether to show the death screen.
    @Inject(method = "die", at = @At("HEAD"))
    private void apoli$sendInstantRespawn(DamageSource source, CallbackInfo ci) {
        InstantRespawnPower.beforeDeath((ServerPlayer) (Object) this);
    }
}
