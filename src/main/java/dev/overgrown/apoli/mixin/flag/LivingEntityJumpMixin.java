package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyJumpHandler;
import dev.overgrown.apoli.power.builtin.ModifySwimSpeedHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @ModifyReturnValue(method = "getJumpPower()F", at = @At("RETURN"))
    private float apoli$modifyJumpPower(float original) {
        return ModifyJumpHandler.modify((LivingEntity) (Object) this, original);
    }

    @Inject(method = "jumpFromGround()V", at = @At("TAIL"))
    private void apoli$onJumpFromGround(CallbackInfo ci) {
        ModifyJumpHandler.onJump((LivingEntity) (Object) this);
    }

    @ModifyReturnValue(method = "getWaterSlowDown()F", at = @At("RETURN"))
    private float apoli$modifySwimSpeed(float original) {
        return ModifySwimSpeedHandler.modifyWaterDrag((LivingEntity) (Object) this, original);
    }

    @ModifyConstant(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        constant = @Constant(floatValue = 0.9F, ordinal = 0)
    )
    private float apoli$modifySprintingSwimSpeed(float original) {
        return ModifySwimSpeedHandler.modifyWaterDrag((LivingEntity) (Object) this, original);
    }
}
