package dev.overgrown.apoli.mixin.possession;

import dev.overgrown.apoli.entity.possession.Possession;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobPossessionMixin {

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void apoli$possessedHostWaits(CallbackInfo ci) {
        if (Possession.possessorOf((Mob) (Object) this) != null) ci.cancel();
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void apoli$possessedHostStays(CallbackInfo ci) {
        if (Possession.possessorOf((Mob) (Object) this) != null) ci.cancel();
    }
}
