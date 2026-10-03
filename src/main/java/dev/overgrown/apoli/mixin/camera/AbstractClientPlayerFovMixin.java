package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.power.builtin.ModifyFovPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
@Environment(EnvType.CLIENT)
public abstract class AbstractClientPlayerFovMixin {

    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At(value = "RETURN", ordinal = 0))
    private float apoli$modifyScopedFov(float original) {
        if (ClientResolvedPowers.isEmpty()) return original;
        return ModifyFovPower.apply((AbstractClientPlayer) (Object) this, original, ClientResolvedPowers::isActive);
    }

    @WrapOperation(method = "getFieldOfViewModifier",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float apoli$modifyFov(float delta, float start, float end, Operation<Float> original) {
        if (ClientResolvedPowers.isEmpty()) return original.call(delta, start, end);
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        float modified = ModifyFovPower.apply(self, end, ClientResolvedPowers::isActive);
        float scale = ModifyFovPower.ignoresEffectScale(self, ClientResolvedPowers::isActive) ? 1.0F : delta;
        return original.call(scale, start, modified);
    }
}
