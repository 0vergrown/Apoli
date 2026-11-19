package dev.overgrown.apoli.mixin.possession;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.entity.possession.Possession;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPossessionMixin {

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V",
        shift = At.Shift.AFTER))
    private void apoli$followPossessor(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity possessor = Possession.possessorOf(self);
        if (possessor == null || self.level().isClientSide()) return;
        self.setPos(possessor.getX(), possessor.getY(), possessor.getZ());
        self.setDeltaMovement(Vec3.ZERO);
        self.setYRot(possessor.getYRot());
        self.setXRot(possessor.getXRot());
        if (possessor instanceof LivingEntity living) {
            self.yHeadRot = living.yHeadRot;
            self.yBodyRot = living.yBodyRot;
        }
        self.resetFallDistance();
        self.setOnGround(possessor.onGround());
        self.setShiftKeyDown(possessor.isShiftKeyDown());
        self.setSprinting(possessor.isSprinting());
        self.setSwimming(possessor.isSwimming());
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void apoli$knockPossessor(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity possessor = Possession.possessorOf(self);
        if (possessor == null || self.level().isClientSide()) return;
        if (possessor instanceof LivingEntity living) {
            double scaled = strength * (1.0 - self.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (scaled > 0.0) {
                living.knockback(scaled, x, z);
                living.hurtMarked = true;
            }
        }
        ci.cancel();
    }

    @ModifyReturnValue(method = "canBreatheUnderwater", at = @At("RETURN"))
    private boolean apoli$breatheAsHost(boolean original) {
        if (original) return true;
        Entity host = Possession.hostOf((LivingEntity) (Object) this);
        return host instanceof LivingEntity living && living.canBreatheUnderwater();
    }

    @ModifyReturnValue(method = "canBeSeenAsEnemy", at = @At("RETURN"))
    private boolean apoli$possessorUnseen(boolean original) {
        return original && Possession.hostOf((LivingEntity) (Object) this) == null;
    }

    @ModifyReturnValue(method = "isPickable", at = @At("RETURN"))
    private boolean apoli$possessorUnpickable(boolean original) {
        return original && Possession.hostOf((LivingEntity) (Object) this) == null;
    }
}
