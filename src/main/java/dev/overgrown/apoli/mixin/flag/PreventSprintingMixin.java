package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class PreventSprintingMixin {

    @ModifyVariable(method = "setSprinting(Z)V", at = @At("HEAD"), argsOnly = true)
    private boolean apoli$preventSprinting(boolean sprinting) {
        if (!sprinting) return false;
        return !PowerLookup.hasActive((LivingEntity) (Object) this, ApoliIds.PREVENT_SPRINTING);
    }
}
