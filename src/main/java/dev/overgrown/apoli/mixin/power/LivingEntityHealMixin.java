package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyHealingHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {

    @ModifyVariable(method = "heal(F)V", at = @At("HEAD"), argsOnly = true)
    private float apoli$modifyHealing(float amount) {
        return ModifyHealingHandler.modify((LivingEntity) (Object) this, amount);
    }
}
