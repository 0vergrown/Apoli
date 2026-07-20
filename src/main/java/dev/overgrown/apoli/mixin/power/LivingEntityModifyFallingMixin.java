package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyFallingHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntity.class)
public abstract class LivingEntityModifyFallingMixin {

    @ModifyConstant(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", constant = @Constant(doubleValue = 0.08))
    private double apoli$modifyFallingGravity(double original) {
        return ModifyFallingHandler.modifyGravity((LivingEntity) (Object) this, original);
    }
}
