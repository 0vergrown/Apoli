package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Map;

public final class StatusEffectInstanceCodec {
    public static final Codec<MobEffectInstance> CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<MobEffectInstance>mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(MobEffectInstance::getEffect),
            Codec.INT.optionalFieldOf("duration", 100).forGetter(MobEffectInstance::getDuration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance::getAmplifier),
            Codec.BOOL.optionalFieldOf("is_ambient", false).forGetter(MobEffectInstance::isAmbient),
            Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance::isVisible),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(MobEffectInstance::showIcon)
        ).apply(instance, StatusEffectInstanceCodec::build)),
        Map.of("id", "effect", "ambient", "is_ambient")
    ).codec();

    private StatusEffectInstanceCodec() {}

    private static MobEffectInstance build(MobEffect effect, int duration, int amplifier,
                                            boolean isAmbient, boolean showParticles, boolean showIcon) {
        return new MobEffectInstance(effect, duration, amplifier, isAmbient, showParticles, showIcon);
    }
}
