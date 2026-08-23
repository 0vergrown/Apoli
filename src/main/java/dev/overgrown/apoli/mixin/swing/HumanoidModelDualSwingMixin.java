package dev.overgrown.apoli.mixin.swing;

import dev.overgrown.apoli.access.DualSwingHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
@Environment(EnvType.CLIENT)
public abstract class HumanoidModelDualSwingMixin<T extends LivingEntity> {

    @Shadow public ModelPart head;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(method = "setupAttackAnimation", at = @At("TAIL"))
    private void apoli$swingOtherArm(T entity, float ageInTicks, CallbackInfo ci) {
        float attackTime = ((EntityModel<?>) (Object) this).attackTime;
        if (attackTime <= 0.0F || !DualSwingHolder.of(entity)) return;

        HumanoidArm attacking = entity.swingingArm == InteractionHand.MAIN_HAND
            ? entity.getMainArm()
            : entity.getMainArm().getOpposite();
        ModelPart other = attacking == HumanoidArm.RIGHT ? this.leftArm : this.rightArm;

        float eased = 1.0F - attackTime;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;

        float lift = Mth.sin(eased * (float) Math.PI);
        float pitch = Mth.sin(attackTime * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;

        other.xRot -= lift * 1.2F + pitch;
        other.yRot += this.body.yRot * 2.0F;
        other.zRot += Mth.sin(attackTime * (float) Math.PI) * -0.4F;
    }
}
