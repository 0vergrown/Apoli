package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyVelocityHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityModifyVelocityMixin {

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 apoli$modifyMovementVelocity(Vec3 movement, MoverType moverType) {
        if (moverType != MoverType.SELF) {
            return movement;
        }
        return ModifyVelocityHandler.modify((Entity) (Object) this, movement);
    }
}
