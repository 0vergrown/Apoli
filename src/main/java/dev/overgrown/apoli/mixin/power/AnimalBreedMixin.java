package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ActionOnBreedPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalBreedMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V",
        shift = At.Shift.AFTER))
    private void apoli$actionOnBreed(ServerLevel level, Animal mate, CallbackInfo ci, @Local AgeableMob baby) {
        ActionOnBreedPower.fire((Animal) (Object) this, mate, baby);
    }
}
