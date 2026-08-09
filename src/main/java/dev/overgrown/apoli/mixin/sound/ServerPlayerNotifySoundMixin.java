package dev.overgrown.apoli.mixin.sound;

import dev.overgrown.apoli.sound.SoundReplacer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerNotifySoundMixin {

    @Shadow public ServerGamePacketListenerImpl connection;

    @Inject(method = "playNotifySound(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V", at = @At("HEAD"), cancellable = true)
    private void apoli$replaceNotifySound(SoundEvent sound, SoundSource source, float volume, float pitch,
                                          CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!SoundReplacer.hasEmission(self)) return;
        ci.cancel();
        SoundReplacer.emit(self, sound, volume, pitch, (replaced, newVolume, newPitch) ->
            this.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(replaced), source,
                self.getX(), self.getY(), self.getZ(), newVolume, newPitch, self.getRandom().nextLong())));
    }
}
