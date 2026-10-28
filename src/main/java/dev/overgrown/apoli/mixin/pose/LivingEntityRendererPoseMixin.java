package dev.overgrown.apoli.mixin.pose;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overgrown.apoli.client.render.ForcedPoseShapes;
import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.data.EntityPoses;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererPoseMixin {

    private static final String SETUP_ROTATIONS = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V";
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Shadow
    protected EntityModel<?> model;

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;isAutoSpinAttack()Z"))
    private boolean apoli$forceRiptidePose(boolean original, @Local(argsOnly = true) LivingEntity entity) {
        if (ModelPartAnimator.overridesPose(entity)) return false;
        return original || PosePower.hasEntityPose(entity, Pose.SPIN_ATTACK);
    }

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;hasPose(Lnet/minecraft/world/entity/Pose;)Z"))
    private boolean apoli$neutralizeSleepRotation(boolean original, @Local(argsOnly = true) LivingEntity entity) {
        return original && !ModelPartAnimator.overridesPose(entity);
    }

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/entity/LivingEntity;deathTime:I"))
    private int apoli$forceDyingPose(int original, @Local(argsOnly = true) LivingEntity entity) {
        int forced = EntityPoses.deathTime(entity, original);
        if (forced == original || ModelPartAnimator.overridesPose(entity)) return original;
        return forced;
    }

    @Inject(method = SETUP_ROTATIONS, at = @At("TAIL"))
    private void apoli$forcedPoseFrame(LivingEntity entity, PoseStack poseStack, float bob, float yBodyRot,
                                       float partialTick, float scale, CallbackInfo ci) {
        Pose pose = EntityPoses.forced(entity);
        if (pose == null || !EntityPoses.movesBody(pose) || !(this.model instanceof HumanoidModel)) return;
        if (ModelPartAnimator.overridesPose(entity)) return;
        float progress = EntityPoses.progress(entity, partialTick);
        float pitch = EntityPoses.bodyPitch(pose) * progress;
        if (pitch != 0.0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        float modelScale = (Object) this instanceof PlayerRenderer ? EntityPoses.PLAYER_MODEL_SCALE : 1.0F;
        poseStack.translate(0.0F, -EntityPoses.bodyDrop(pose) * progress / 16.0F * modelScale, 0.0F);
    }

    @Inject(method = RENDER, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void apoli$shapeMobPose(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!(this.model instanceof HumanoidModel<?> humanoid) || humanoid instanceof PlayerModel) return;
        Pose pose = EntityPoses.forced(entity);
        if (pose == null || !EntityPoses.shapes(pose) || ModelPartAnimator.overridesPose(entity)) return;
        ForcedPoseShapes.apply(humanoid, entity, pose, entity.tickCount + partialTick);
    }
}
