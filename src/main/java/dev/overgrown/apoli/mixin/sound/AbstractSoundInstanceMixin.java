package dev.overgrown.apoli.mixin.sound;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.sound.ReplaceableSound;
import dev.overgrown.apoli.sound.SoundVariants;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractSoundInstance.class)
public abstract class AbstractSoundInstanceMixin implements ReplaceableSound {

    @Mutable
    @Shadow @Final protected ResourceLocation location;

    @Shadow protected float volume;

    @Shadow protected float pitch;

    @Shadow @Final protected RandomSource random;

    @Unique
    private int apoli$variant;

    @Inject(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/util/RandomSource;)V",
        at = @At("TAIL"))
    private void apoli$unwrapVariant(ResourceLocation location, SoundSource source, RandomSource random, CallbackInfo ci) {
        if (!SoundVariants.isVariant(location)) return;
        ResourceLocation base = SoundVariants.base(location);
        int index = SoundVariants.index(location);
        if (base == null || index < 0) return;
        this.location = base;
        this.apoli$variant = index + 1;
    }

    @ModifyExpressionValue(method = "resolve", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/sounds/WeighedSoundEvents;getSound(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/resources/sounds/Sound;"))
    private Sound apoli$pickVariant(Sound original, @Local WeighedSoundEvents events) {
        if (this.apoli$variant == 0) return original;
        List<Weighted<Sound>> entries = ((WeighedSoundEventsAccessor) events).apoli$getList();
        int index = this.apoli$variant - 1;
        return index < entries.size() ? entries.get(index).getSound(this.random) : original;
    }

    @Override
    public void apoli$replaceWith(ResourceLocation location, float volume, float pitch) {
        if (!location.equals(this.location)) this.apoli$variant = 0;
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
