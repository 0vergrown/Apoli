package dev.overgrown.apoli.mixin.pose;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class LocalPlayerCrouchMixin {

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;isShiftKeyDown()Z"))
    private boolean apoli$forceCrouching(boolean original) {
        return original || PosePower.hasEntityPose((LocalPlayer) (Object) this, Pose.CROUCHING);
    }
}
