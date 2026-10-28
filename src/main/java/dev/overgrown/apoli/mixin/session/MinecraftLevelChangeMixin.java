package dev.overgrown.apoli.mixin.session;

import dev.overgrown.apoli.ApoliClient;
import dev.overgrown.apoli.client.rope.RopeClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftLevelChangeMixin {

    @Inject(method = "updateLevelInEngines", at = @At("HEAD"))
    private void apoli$onLevelChange(@Nullable ClientLevel level, CallbackInfo ci) {
        if (level == null) ApoliClient.clearSession();
        else RopeClientManager.clear();
    }
}
