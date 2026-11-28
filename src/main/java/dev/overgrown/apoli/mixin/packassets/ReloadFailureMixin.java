package dev.overgrown.apoli.mixin.packassets;

import dev.overgrown.apoli.packassets.client.ClientPackAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// When a resource reload fails the game turns its resource packs off and reloads. The data pack assets pack is
// always on, so it has to be taken out here or a broken pack would fail every reload, including the next start.
@Mixin(Minecraft.class)
public abstract class ReloadFailureMixin {
    @Inject(method = "clearResourcePacksOnError", at = @At("HEAD"))
    private void apoli$dropAssetsOnReloadFailure(Throwable exception, Component message, CallbackInfo ci) {
        ClientPackAssets.onReloadFailed();
    }
}
