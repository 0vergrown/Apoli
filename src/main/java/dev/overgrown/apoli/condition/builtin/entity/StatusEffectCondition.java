package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class StatusEffectCondition implements ConditionType<EntityCtx, StatusEffectCondition.Cfg> {
    public record Cfg(
        ResourceLocation effect,
        Optional<Integer> minAmplifier,
        Optional<Integer> maxAmplifier,
        Optional<Integer> minDuration,
        Optional<Integer> maxDuration
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(Cfg::effect),
            Codec.INT.optionalFieldOf("min_amplifier").forGetter(Cfg::minAmplifier),
            Codec.INT.optionalFieldOf("max_amplifier").forGetter(Cfg::maxAmplifier),
            Codec.INT.optionalFieldOf("min_duration").forGetter(Cfg::minDuration),
            Codec.INT.optionalFieldOf("max_duration").forGetter(Cfg::maxDuration)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT
            .getHolder(ResourceKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT, cfg.effect))
            .orElse(null);
        if (holder == null) return false;
        LivingEntity living = ctx.living();
        if (living == null) return false;
        MobEffectInstance instance = living.getEffect(holder);
        if (instance == null) return false;
        if (cfg.minAmplifier.isPresent() && instance.getAmplifier() < cfg.minAmplifier.get()) return false;
        if (cfg.maxAmplifier.isPresent() && instance.getAmplifier() > cfg.maxAmplifier.get()) return false;
        if (cfg.minDuration.isPresent() && instance.getDuration() < cfg.minDuration.get()) return false;
        if (cfg.maxDuration.isPresent() && instance.getDuration() > cfg.maxDuration.get()) return false;
        return true;
    }
}
