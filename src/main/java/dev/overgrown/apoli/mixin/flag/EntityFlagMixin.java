package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.GroundedPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityFlagMixin {
    @ModifyReturnValue(method = "onGround", at = @At("RETURN"))
    private boolean apoli$grounded(boolean original) {
        return original || (Object) this instanceof LivingEntity living && GroundedPower.holds(living);
    }
}
