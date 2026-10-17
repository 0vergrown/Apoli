package dev.overgrown.apoli.mixin.custom_model;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import dev.overgrown.apoli.power.builtin.ElytraFlightPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CapeLayer.class)
@Environment(EnvType.CLIENT)
public abstract class CapeLayerHideMixin {

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isModelPartShown(Lnet/minecraft/world/entity/player/PlayerModelPart;)Z"))
    private boolean apoli$hideCape(boolean shown, @Local(argsOnly = true) AbstractClientPlayer player) {
        return shown && !CustomModelRenderPower.shouldHideCape(player) && !ElytraFlightPower.shouldRenderElytra(player);
    }
}
