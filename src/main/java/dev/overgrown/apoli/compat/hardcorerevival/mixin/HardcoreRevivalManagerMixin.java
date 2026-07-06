package dev.overgrown.apoli.compat.hardcorerevival.mixin;

import dev.overgrown.apoli.compat.hardcorerevival.power.ActionOnRevivePower;
import net.blay09.mods.hardcorerevival.HardcoreRevivalManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(HardcoreRevivalManager.class)
public abstract class HardcoreRevivalManagerMixin {

    @Inject(method = "wakeup(Lnet/minecraft/world/entity/player/Player;Z)V", at = @At("HEAD"))
    private void apoli$onWakeup(Player player, boolean applyEffects, CallbackInfo ci) {
        HardcoreRevivalManager self = (HardcoreRevivalManager) (Object) this;
        if (self.getRevivalData(player).isKnockedOut()) {
            ActionOnRevivePower.handleRevived(player);
        }
    }
}
