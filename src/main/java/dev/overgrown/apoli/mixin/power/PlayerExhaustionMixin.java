package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyExhaustionHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float apoli$modifyExhaustion(float exhaustion) {
        return ModifyExhaustionHandler.modify((Player) (Object) this, exhaustion);
    }
}
