package dev.overgrown.apoli.mixin.sound;

import dev.overgrown.apoli.sound.SoundTracker;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSoundTrackMixin {

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
        at = @At("HEAD"))
    private void apoli$trackPositionedSound(@Nullable Player except, double x, double y, double z,
                                            Holder<SoundEvent> sound, SoundSource source,
                                            float volume, float pitch, long seed, CallbackInfo ci) {
        if (!SoundTracker.watching()) return;
        SoundEvent event = sound.value();
        SoundTracker.record((Level) (Object) this, event.getLocation(), source, x, y, z, event.getRange(volume));
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
        at = @At("HEAD"))
    private void apoli$trackEntitySound(@Nullable Player except, Entity entity, Holder<SoundEvent> sound,
                                        SoundSource source, float volume, float pitch, long seed, CallbackInfo ci) {
        if (!SoundTracker.watching()) return;
        SoundEvent event = sound.value();
        SoundTracker.record((Level) (Object) this, event.getLocation(), source,
            entity.getX(), entity.getY(), entity.getZ(), event.getRange(volume));
    }
}
