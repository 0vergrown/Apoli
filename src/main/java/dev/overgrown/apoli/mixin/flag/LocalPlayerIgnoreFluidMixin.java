package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.IgnoreFluidPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class LocalPlayerIgnoreFluidMixin {
    @ModifyReturnValue(method = "isUnderWater", at = @At("RETURN"))
    private boolean apoli$ignoredWaterIsNotUnderwater(boolean original) {
        if (!original) return false;
        LocalPlayer self = (LocalPlayer) (Object) this;
        return self.isInWater() || !IgnoreFluidPower.holds(self);
    }
}
