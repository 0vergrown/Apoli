package dev.overgrown.apoli.mixin.pose;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.client.render.ModelPartAnimator;
import dev.overgrown.apoli.power.builtin.PosePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererPoseMixin {

    private static final String SETUP_ROTATIONS = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V";

    @ModifyExpressionValue(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;isAutoSpinAttack()Z"))
    private boolean apoli$forceRiptidePose(boolean original, @Local(argsOnly = true) LivingEntity entity) {
        if (ModelPartAnimator.overridesPose(entity)) return false;
        return original || PosePower.hasEntityPose(entity, Pose.SPIN_ATTACK);
    }

    @WrapOperation(method = SETUP_ROTATIONS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;hasPose(Lnet/minecraft/world/entity/Pose;)Z"))
    private boolean apoli$neutralizeSleepRotation(LivingEntity entity, Pose pose, Operation<Boolean> original) {
        return !ModelPartAnimator.overridesPose(entity) && original.call(entity, pose);
    }
}
