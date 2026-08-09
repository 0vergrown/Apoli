package dev.overgrown.apoli.mixin.sound;

import dev.overgrown.apoli.sound.ReplaceableSound;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractSoundInstance.class)
public abstract class AbstractSoundInstanceMixin implements ReplaceableSound {

    @Mutable
    @Shadow @Final protected ResourceLocation location;

    @Shadow protected float volume;

    @Shadow protected float pitch;

    @Override
    public void apoli$replaceWith(ResourceLocation location, float volume, float pitch) {
        this.location = location;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public float apoli$baseVolume() {
        return this.volume;
    }

    @Override
    public float apoli$basePitch() {
        return this.pitch;
    }
}
