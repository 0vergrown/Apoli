package dev.overgrown.apoli.mixin.scale;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.ScaleRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class EntityRendererScaleMixin {

    @Inject(method = "renderNameTag", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void apoli$nameTagInWorldUnits(Entity entity, Component name, PoseStack poseStack,
                                           MultiBufferSource buffers, int light, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntityRenderer) return;
        ScaleRender.undoOuterScale(entity, poseStack, Minecraft.getInstance().getFrameTime());
    }

    @ModifyExpressionValue(method = "renderNameTag", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;getNameTagOffsetY()F"))
    private float apoli$nameTagFollowsModel(float offset, @Local(argsOnly = true) Entity entity) {
        return ScaleRender.nameTagOffset(entity, offset, Minecraft.getInstance().getFrameTime());
    }
}
