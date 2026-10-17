package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.power.builtin.ModifyFovPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractClientPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class AbstractClientPlayerFovMixin {

    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At(value = "RETURN", ordinal = 0))
    private float apoli$modifyScopedFov(float original) {
        if (ClientResolvedPowers.isEmpty()) return original;
        return ModifyFovPower.apply((AbstractClientPlayer) (Object) this, original, ClientResolvedPowers::isActive);
    }

    @ModifyArg(method = "getFieldOfViewModifier",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"), index = 0)
    private float apoli$fovEffectScale(float delta, float start, float end) {
        if (ClientResolvedPowers.isEmpty()) return delta;
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        return ModifyFovPower.ignoresEffectScale(self, ClientResolvedPowers::isActive) ? 1.0F : delta;
    }

    @ModifyArg(method = "getFieldOfViewModifier",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"), index = 2)
    private float apoli$modifyFov(float delta, float start, float end) {
        if (ClientResolvedPowers.isEmpty()) return end;
        return ModifyFovPower.apply((AbstractClientPlayer) (Object) this, end, ClientResolvedPowers::isActive);
    }
}
