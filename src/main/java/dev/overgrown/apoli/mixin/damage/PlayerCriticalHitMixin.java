package dev.overgrown.apoli.mixin.damage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.data.CriticalHitContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerCriticalHitMixin {

    @WrapOperation(method = "attack(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean apoli$markCriticalHit(Entity target, DamageSource source, float amount,
                                          Operation<Boolean> original, @Local(ordinal = 2) boolean critical) {
        Player self = (Player) (Object) this;
        long previous = CriticalHitContext.set(self.getId(), critical);
        try {
            return original.call(target, source, amount);
        } finally {
            CriticalHitContext.restore(previous);
        }
    }
}
