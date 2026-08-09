package dev.overgrown.apoli.compat.skinlayers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
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
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CustomLayerFeatureRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class CustomLayerFeatureRendererMixin {

    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
        + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
        + "Lnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V";

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

    @ModifyArgs(method = "renderLayers", at = @At(value = "INVOKE",
        target = "Ldev/tr7zw/skinlayers/api/Mesh;render(Lnet/minecraft/client/model/geom/ModelPart;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void apoli$tintMesh(Args args) {
        ModelPart part = args.get(0);
        args.set(4, SkinRenderCompat.overlay(args.<Integer>get(4), SkinLayersState.current(), part));
        args.set(5, SkinRenderCompat.tint(args.<Integer>get(5), SkinLayersState.current(), part));
    }
}
