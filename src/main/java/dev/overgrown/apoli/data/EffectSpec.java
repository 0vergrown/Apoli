package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record EffectSpec(
    MobEffect effect,
    Expression duration,
    Expression amplifier,
    boolean ambient,
    boolean showParticles,
    boolean showIcon
) {
    public static final Codec<EffectSpec> CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<EffectSpec>mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("effect").forGetter(EffectSpec::effect),
            Expression.INT_OR_EXPR.optionalFieldOf("duration", Expression.constant(100)).forGetter(EffectSpec::duration),
            Expression.INT_OR_EXPR.optionalFieldOf("amplifier", Expression.constant(0)).forGetter(EffectSpec::amplifier),
            Codec.BOOL.optionalFieldOf("is_ambient", false).forGetter(EffectSpec::ambient),
            Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(EffectSpec::showParticles),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(EffectSpec::showIcon)
        ).apply(instance, EffectSpec::new)),
        Map.of("id", "effect", "ambient", "is_ambient")
    ).codec();

    public static final Codec<List<EffectSpec>> LIST_OR_SINGLE = Codec.either(CODEC, Codec.list(CODEC)).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
    );

    public MobEffectInstance resolve(@Nullable Entity target) {
        return new MobEffectInstance(effect, duration.evalInt(target), amplifier.evalInt(target), ambient, showParticles, showIcon);
    }

    public MobEffectInstance resolveWithDuration(@Nullable Entity target, int durationTicks) {
        return new MobEffectInstance(effect, durationTicks, amplifier.evalInt(target), ambient, showParticles, showIcon);
    }
}
