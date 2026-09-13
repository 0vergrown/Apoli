package dev.overgrown.apoli.mixin.sound;

import dev.overgrown.apoli.sound.SoundTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineTrackMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void apoli$trackReceivedSound(SoundInstance instance, CallbackInfo ci) {
        if (!SoundTracker.watching()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !instance.canPlaySound()) return;
        SoundTracker.record(level, instance.getLocation(), instance.getSource(),
            instance.getX(), instance.getY(), instance.getZ(), SoundTracker.UNKNOWN_RANGE);
    }
}
