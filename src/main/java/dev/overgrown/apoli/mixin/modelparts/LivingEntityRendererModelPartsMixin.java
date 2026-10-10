package dev.overgrown.apoli.mixin.modelparts;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.access.PartVisibilityUndo;
import dev.overgrown.apoli.client.particle.RenderedPoses;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRendererModelPartsMixin {

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String RENDER_TO_BUFFER = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    @Shadow
    protected EntityModel<?> model;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void apoli$beginModelParts(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffers, int light, CallbackInfo ci) {
        RenderedPoses.begin(entity, poseStack);
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = RENDER_TO_BUFFER))
    private void apoli$captureModelParts(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffers, int light, CallbackInfo ci) {
        RenderedPoses.capture((EntityRenderer<?>) (Object) this, entity, poseStack, this.model, partialTick);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void apoli$endModelParts(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                     MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (this.model instanceof PartVisibilityUndo undo) undo.apoli$undoPartVisibility();
        RenderedPoses.end();
    }
}
