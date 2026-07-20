package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyFallingHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityModifyFallingMixin {

    @ModifyReturnValue(method = "getDefaultGravity", at = @At("RETURN"))
    private double apoli$modifyFallingGravity(double original) {
        return ModifyFallingHandler.modifyGravity((LivingEntity) (Object) this, original);
    }
}
