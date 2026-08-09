package dev.overgrown.apoli.mixin.sound;

import dev.overgrown.apoli.sound.ReplaceableSound;
import dev.overgrown.apoli.sound.SoundReplacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineReceptionMixin {

    @Unique
    private static boolean apoli$reentrant;

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
        at = @At("HEAD"), cancellable = true)
    private void apoli$replaceReceivedSound(SoundInstance instance, CallbackInfo ci) {
        if (apoli$reentrant) return;
        if (!(instance instanceof AbstractSoundInstance)) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !SoundReplacer.hasReception(player)) return;

        ReplaceableSound access = (ReplaceableSound) instance;
        SoundEngine self = (SoundEngine) (Object) this;
        SoundEvent original = SoundEvent.createVariableRangeEvent(instance.getLocation());
        boolean[] first = {true};

        apoli$reentrant = true;
        try {
            SoundReplacer.receive(player, original, access.apoli$baseVolume(), access.apoli$basePitch(),
                (replaced, volume, pitch) -> {
                    if (first[0]) {
                        first[0] = false;
                        access.apoli$replaceWith(replaced.getLocation(), volume, pitch);
                        self.play(instance);
                    } else {
                        self.play(new SimpleSoundInstance(replaced.getLocation(), instance.getSource(),
                            volume, pitch, SoundInstance.createUnseededRandom(), instance.isLooping(),
                            instance.getDelay(), instance.getAttenuation(),
                            instance.getX(), instance.getY(), instance.getZ(), instance.isRelative()));
                    }
                });
        } finally {
            apoli$reentrant = false;
        }
        ci.cancel();
    }
}
