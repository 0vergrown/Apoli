package dev.overgrown.apoli.mixin.sound;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.client.ClientResolvedPowers;
import dev.overgrown.apoli.power.builtin.ModifyHearingRangePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
@Environment(EnvType.CLIENT)
public abstract class SoundEngineHearingRangeMixin {

    @ModifyExpressionValue(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float apoli$hearingRange(float volumeFactor, @Local(argsOnly = true) SoundInstance instance,
                                     @Local Sound sound) {
        if (ClientResolvedPowers.isEmpty()) return volumeFactor;
        if (instance.isRelative() || instance.getAttenuation() != SoundInstance.Attenuation.LINEAR) return volumeFactor;
        int distance = sound.getAttenuationDistance();
        if (distance <= 0) return volumeFactor;
        double base = (double) volumeFactor * distance;
        double range = ModifyHearingRangePower.resolvedSoundRange(Minecraft.getInstance().player, base,
            ClientResolvedPowers::isActive);
        if (range == base) return volumeFactor;
        return (float) (Math.max(range, 0.001) / distance);
    }
}
