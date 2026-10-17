package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.CreativeFlightPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerCreativeFlightFallMixin {

    @ModifyExpressionValue(method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;mayFly()Z"))
    private boolean apoli$powerFlightTakesFallDamage(boolean mayfly) {
        return mayfly && !CreativeFlightPower.providesFlight((Player) (Object) this);
    }
}
