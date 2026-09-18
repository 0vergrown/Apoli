package dev.overgrown.apoli.mixin.input;

import dev.overgrown.apoli.power.builtin.ModifyUseSlowdownPower;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerUseSlowdownMixin {

    @ModifyConstant(method = "aiStep()V", constant = @Constant(floatValue = 0.2F))
    private float apoli$modifyUseSlowdown(float original) {
        return ModifyUseSlowdownPower.compute((LocalPlayer) (Object) this, original);
    }
}
