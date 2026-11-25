package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyFallingHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityModifyFallingMixin {

    @ModifyExpressionValue(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "CONSTANT", args = "doubleValue=0.08"))
    private double apoli$modifyFallingGravity(double original) {
        return ModifyFallingHandler.modifyGravity((LivingEntity) (Object) this, original);
    }
}
