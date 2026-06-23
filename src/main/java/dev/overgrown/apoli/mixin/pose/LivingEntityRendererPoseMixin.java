package dev.overgrown.apoli.mixin.pose;

import dev.overgrown.apoli.power.builtin.PosePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererPoseMixin {

    @Redirect(method = "setupRotations", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;isAutoSpinAttack()Z"))
    private boolean apoli$forceRiptidePose(LivingEntity entity) {
        return entity.isAutoSpinAttack() || PosePower.hasEntityPose(entity, Pose.SPIN_ATTACK);
    }
}
