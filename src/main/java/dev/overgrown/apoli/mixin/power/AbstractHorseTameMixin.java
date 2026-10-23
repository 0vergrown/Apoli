package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ActionOnTamePower;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseTameMixin {

    @Inject(method = "tameWithName", at = @At("RETURN"))
    private void apoli$actionOnTame(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) ActionOnTamePower.fire(player, (AbstractHorse) (Object) this);
    }
}
