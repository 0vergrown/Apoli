package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ActionOnTamePower;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalTameMixin {

    @Inject(method = "tame", at = @At("TAIL"))
    private void apoli$actionOnTame(Player player, CallbackInfo ci) {
        ActionOnTamePower.fire(player, (TamableAnimal) (Object) this);
    }
}
