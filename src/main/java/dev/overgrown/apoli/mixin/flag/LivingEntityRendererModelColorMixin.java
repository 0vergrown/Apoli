package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.LevelRenderPass;
import dev.overgrown.apoli.client.render.ModelColorState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRendererModelColorMixin {

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String RENDER_TO_BUFFER = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void apoli$beginModelColor(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack pose, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ModelColorState.begin(entity, ((LivingEntityRenderer<?, ?>) (Object) this).getModel());
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void apoli$endModelColor(LivingEntity entity, float entityYaw, float partialTick,
                                     PoseStack pose, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ModelColorState.end();
    }

    @ModifyArg(method = RENDER, at = @At(value = "INVOKE", target = RENDER_TO_BUFFER), index = 3)
    private int apoli$whitenModel(int overlay) {
        return ModelColorState.whitens() ? OverlayTexture.pack(15, overlay >> 16 & 0xFFFF) : overlay;
    }

    @ModifyArg(method = RENDER, at = @At(value = "INVOKE", target = RENDER_TO_BUFFER), index = 4)
    private int apoli$tintModel(int color) {
        float[] tint = ModelColorState.tint();
        if (tint == null) return color;
        return FastColor.ARGB32.color(
            Math.min(255, Math.round(FastColor.ARGB32.alpha(color) * tint[3])),
            Math.min(255, Math.round(FastColor.ARGB32.red(color) * tint[0])),
            Math.min(255, Math.round(FastColor.ARGB32.green(color) * tint[1])),
            Math.min(255, Math.round(FastColor.ARGB32.blue(color) * tint[2])));
    }

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType apoli$translucentModel(RenderType original, LivingEntity entity, boolean isVisible,
                                              boolean isInvisibleToPlayer, boolean glowing) {
        if (!isVisible || isInvisibleToPlayer || ModelColorState.minAlpha(entity) >= 0.999f) return original;
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, ?> self = (LivingEntityRenderer<LivingEntity, ?>) (Object) this;
        ResourceLocation texture = self.getTextureLocation(entity);
        return LevelRenderPass.active() ? RenderType.itemEntityTranslucentCull(texture) : RenderType.entityTranslucentCull(texture);
    }
}
