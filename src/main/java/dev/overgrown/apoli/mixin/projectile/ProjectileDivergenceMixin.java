package dev.overgrown.apoli.mixin.projectile;

import dev.overgrown.apoli.power.builtin.ModifyProjectileDivergencePower;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Projectile.class)
public abstract class ProjectileDivergenceMixin {

    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float apoli$modifyDivergence(float divergence) {
        return ModifyProjectileDivergencePower.modify((Projectile) (Object) this, divergence);
    }
}
