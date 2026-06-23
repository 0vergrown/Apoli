package dev.overgrown.apoli.mixin.damage;

import dev.overgrown.apoli.power.builtin.ModifyDamageHandler;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerZeroDamageMixin {
    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float apoli$keepZeroDamageHitForModifiers(float value, DamageSource source, float amount) {
        if (value != 0.0F) return value;
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return value;
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        float preview = ModifyProjectileDamageHandler.previewAmount(attacker, self, source, 0.0F, self.level());
        preview = ModifyDamageHandler.previewAmount(attacker, self, source, preview, self.level());
        return preview > 0.0F ? Float.MIN_VALUE : value;
    }
}
