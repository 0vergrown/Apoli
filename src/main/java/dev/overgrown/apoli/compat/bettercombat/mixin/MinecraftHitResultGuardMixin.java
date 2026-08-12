package dev.overgrown.apoli.compat.bettercombat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftHitResultGuardMixin {

    @Shadow public HitResult hitResult;

    @Shadow public ClientLevel level;

    @Shadow public LocalPlayer player;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void apoli$guardNullHitResult(boolean bl, CallbackInfo ci) {
        if (hitResult != null || level == null || player == null) return;
        hitResult = BlockHitResult.miss(player.getEyePosition(), Direction.UP, player.blockPosition());
    }
}
