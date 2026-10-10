package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.ScaleRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererScaleMixin {

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String SETUP_ROTATIONS = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V";

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ROTATIONS))
    private void apoli$uniformModelScale(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffers, int light, CallbackInfo ci) {
        float width = ScaleRender.modelWidth(entity, partialTick);
        if (width == 1.0F) return;
        if (ScaleRender.lying(entity)) poseStack.translate(0.0F, ScaleRender.MATTRESS_DROP * (width - 1.0F), 0.0F);
        poseStack.scale(width, width, width);
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", target = SETUP_ROTATIONS, shift = At.Shift.AFTER))
    private void apoli$stretchModel(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffers, int light, CallbackInfo ci) {
        float width = ScaleRender.modelWidth(entity, partialTick);
        float height = ScaleRender.modelHeight(entity, partialTick);
        if (height != width) poseStack.scale(1.0F, height / width, 1.0F);
    }

    @ModifyArg(method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
               at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private float apoli$upsideDownInWorldUnits(float y, @Local(argsOnly = true) LivingEntity entity,
                                               @Local(argsOnly = true, ordinal = 2) float partialTick) {
        float width = ScaleRender.modelWidth(entity, partialTick);
        return width == 1.0F ? y : y / width;
    }
}
