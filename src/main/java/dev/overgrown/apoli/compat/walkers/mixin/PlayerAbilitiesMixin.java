package dev.overgrown.apoli.compat.walkers.mixin;

import dev.overgrown.apoli.compat.walkers.power.ShapePowers;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "tocraft.walkers.api.PlayerAbilities", remap = false)
public abstract class PlayerAbilitiesMixin {

    @Inject(method = "useAbility", at = @At("HEAD"), cancellable = true, remap = false)
    private static void apoli$shapeAbilityUse(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        if (ShapePowers.preventShapeAbilityUse(player)) {
            ci.cancel();
            return;
        }
        ShapePowers.fireShapeAbilityUse(player);
    }
}
