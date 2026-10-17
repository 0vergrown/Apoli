package dev.overgrown.apoli.compat.skinlayers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.SkinRenderCompat;
import dev.overgrown.apoli.compat.skinlayers.SkinLayersState;
import dev.tr7zw.skinlayers.renderlayers.CustomLayerFeatureRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomLayerFeatureRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class CustomLayerFeatureRendererMixin {

    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
        + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
        + "Lnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V";
    private static final String MESH_RENDER = "Ldev/tr7zw/skinlayers/api/Mesh;render(Lnet/minecraft/client/model/geom/ModelPart;"
        + "Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    @Inject(method = RENDER, at = @At("HEAD"), cancellable = true)
    private void apoli$gateLayers(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                  float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                  float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (SkinRenderCompat.suppressed(player, SkinRenderCompat.SKIN_LAYERS_3D)) {
            ci.cancel();
            return;
        }
        SkinLayersState.begin(player);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void apoli$clearLayerState(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                       float netHeadYaw, float headPitch, CallbackInfo ci) {
        SkinLayersState.end();
    }

    @ModifyArg(method = "renderLayers", at = @At(value = "INVOKE", target = MESH_RENDER), index = 4)
    private int apoli$overlayMesh(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        return SkinRenderCompat.overlay(overlay, SkinLayersState.current(), part);
    }

    @ModifyArg(method = "renderLayers", at = @At(value = "INVOKE", target = MESH_RENDER), index = 5)
    private int apoli$tintMesh(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color) {
        return SkinRenderCompat.tint(color, SkinLayersState.current(), part);
    }
}
