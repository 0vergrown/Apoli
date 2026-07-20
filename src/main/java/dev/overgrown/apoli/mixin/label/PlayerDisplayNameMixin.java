package dev.overgrown.apoli.mixin.label;

import dev.overgrown.apoli.entity.DisplayNameOverrides;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void apoli$overrideDisplayName(CallbackInfoReturnable<Component> cir) {
        Component override = DisplayNameOverrides.chatNameFor((Player) (Object) this);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
