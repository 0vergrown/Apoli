package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityIsInWallMixin {
    @ModifyReturnValue(method = "isInWall", at = @At("RETURN"))
    private boolean apoli$phasingSuppressInWall(boolean original) {
        if (!original) return false;
        if (!((Object) this instanceof LivingEntity living)) return true;
        if (!PhasingPower.mayHold(living)) return true;
        return !PhasingPower.holdsActive(living);
    }
}
