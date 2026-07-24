package dev.overgrown.apoli.mixin.custom_model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.CustomModelRenderPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
@Environment(EnvType.CLIENT)
public abstract class CapeLayerHideMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void apoli$hideCape(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        if (CustomModelRenderPower.shouldHideCape(player)) {
            ci.cancel();
        }
    }
}
