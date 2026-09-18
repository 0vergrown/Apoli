package dev.overgrown.apoli.mixin.scale;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.scale.ScaleTypes;
import dev.overgrown.apoli.scale.Scales;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
@Environment(EnvType.CLIENT)
public abstract class EntityRenderDispatcherScaleMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
                     target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apoli$scaleModelPush(Entity entity, double x, double y, double z, float yaw, float partialTick,
                                      PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        poseStack.pushPose();
        if (Scales.untouched(entity)) return;
        float width = Scales.applied(entity, ScaleTypes.MODEL_WIDTH, partialTick);
        float height = Scales.applied(entity, ScaleTypes.MODEL_HEIGHT, partialTick);
        if (width != 1.0F || height != 1.0F) poseStack.scale(width, height, width);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                     target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apoli$scaleModelPop(Entity entity, double x, double y, double z, float yaw, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        poseStack.popPose();
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
               index = 6,
               at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"))
    private float apoli$scaleShadow(PoseStack poseStack, MultiBufferSource buffer, Entity entity,
                                           float strength, float partialTick, LevelReader level, float radius) {
        if (Scales.untouched(entity)) return radius;
        return radius * Scales.applied(entity, ScaleTypes.MODEL_WIDTH, partialTick);
    }
}
