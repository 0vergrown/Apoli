package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.SneakyPower;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntitySneakyMixin {

    @ModifyReturnValue(method = "isDiscrete", at = @At("RETURN"))
    private boolean apoli$sneaky(boolean original) {
        return original || SneakyPower.isSneaky((Entity) (Object) this);
    }
}
