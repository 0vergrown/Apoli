package dev.overgrown.apoli.mixin.damage;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.InvulnerabilityHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityInvulnerableMixin {
    @ModifyReturnValue(method = "isInvulnerableTo", at = @At("RETURN"))
    private boolean apoli$invulnerability(boolean original, DamageSource source) {
        if (original || !((Object) this instanceof LivingEntity self) || self.level().isClientSide()) return original;
        return InvulnerabilityHandler.isImmune(self, source, 0.0F, self.level());
    }
}
