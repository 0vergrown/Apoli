package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * In 1.21.1 vanilla ships {@link MobEffectInstance#CODEC}, but that uses
 * different field names ({@code "ambient"}/{@code "visible"}) than the
 * Apoli convention ({@code "is_ambient"}/{@code "show_particles"}). We
 * keep the legacy Apoli field names so data packs are portable across
 * the 1.20.1 and 1.21.1 Apoli builds.
 *
 * <p>The constructor and {@code getEffect()} accessor switched to
 * {@code Holder<MobEffect>} in 1.21+.</p>
 */
public final class StatusEffectInstanceCodec {
    public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(MobEffectInstance::getEffect),
        Codec.INT.optionalFieldOf("duration", 100).forGetter(MobEffectInstance::getDuration),
        Codec.INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance::getAmplifier),
        Codec.BOOL.optionalFieldOf("is_ambient", false).forGetter(MobEffectInstance::isAmbient),
        Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance::isVisible),
        Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(MobEffectInstance::showIcon)
    ).apply(instance, StatusEffectInstanceCodec::build));

    private StatusEffectInstanceCodec() {}

    private static MobEffectInstance build(Holder<MobEffect> effect, int duration, int amplifier,
                                            boolean isAmbient, boolean showParticles, boolean showIcon) {
        return new MobEffectInstance(effect, duration, amplifier, isAmbient, showParticles, showIcon);
    }
}
