package dev.overgrown.apoli.mixin.label;

import dev.overgrown.apoli.entity.DisplayNameOverrides;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabNameMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void apoli$overrideTabName(CallbackInfoReturnable<Component> cir) {
        Component override = DisplayNameOverrides.tabNameFor((ServerPlayer) (Object) this);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
