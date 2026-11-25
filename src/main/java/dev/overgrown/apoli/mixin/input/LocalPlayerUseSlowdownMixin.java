package dev.overgrown.apoli.mixin.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyUseSlowdownPower;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerUseSlowdownMixin {

    @ModifyExpressionValue(method = "aiStep()V", at = @At(value = "CONSTANT", args = "floatValue=0.2F"))
    private float apoli$modifyUseSlowdown(float original) {
        return ModifyUseSlowdownPower.compute((LocalPlayer) (Object) this, original);
    }
}
