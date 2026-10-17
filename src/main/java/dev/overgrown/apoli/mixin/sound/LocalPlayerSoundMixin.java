package dev.overgrown.apoli.mixin.sound;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.sound.SoundReplacer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSoundMixin {

    @WrapOperation(method = {
        "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
        "playNotifySound(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
    }, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void apoli$replaceOwnSound(Level level, double x, double y, double z, SoundEvent sound,
                                       SoundSource source, float volume, float pitch, boolean distanceDelay,
                                       Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        if (!SoundReplacer.hasEmission(self)) {
            original.call(level, x, y, z, sound, source, volume, pitch, distanceDelay);
            return;
        }
        SoundReplacer.emitLocally(self, sound, volume, pitch, (replaced, newVolume, newPitch) ->
            original.call(level, x, y, z, replaced, source, newVolume, newPitch, distanceDelay));
    }
}
