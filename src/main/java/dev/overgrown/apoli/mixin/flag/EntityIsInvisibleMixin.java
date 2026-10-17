package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.InvisibilityPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityIsInvisibleMixin {
    @ModifyReturnValue(method = "isInvisible", at = @At("RETURN"))
    private boolean apoli$invisibilityFlag(boolean original) {
        return original || ((Object) this instanceof LivingEntity self && InvisibilityPower.hidesUnconditionally(self));
    }
}
