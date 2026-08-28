package dev.overgrown.apoli.mixin.firstperson;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.HandRenderPass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererHandPassMixin {

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void apoli$beginHandPass(PoseStack poseStack, MultiBufferSource buffers, int light,
                                     AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                     CallbackInfo ci) {
        HandRenderPass.begin();
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void apoli$endHandPass(PoseStack poseStack, MultiBufferSource buffers, int light,
                                   AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                   CallbackInfo ci) {
        HandRenderPass.end();
    }
}
