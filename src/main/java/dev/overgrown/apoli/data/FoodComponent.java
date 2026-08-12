package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record FoodComponent(
    int hunger,
    float saturation,
    boolean meat,
    boolean alwaysEdible,
    boolean snack,
    Optional<FoodEffect> effect,
    Optional<List<FoodEffect>> effects
) {
    public record FoodEffect(EffectSpec spec, float chance) {
        public static final Codec<FoodEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            EffectSpec.MAP_CODEC.forGetter(FoodEffect::spec),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(FoodEffect::chance)
        ).apply(i, FoodEffect::new));
    }

    public static final Codec<FoodComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("hunger").forGetter(FoodComponent::hunger),
        Codec.FLOAT.fieldOf("saturation").forGetter(FoodComponent::saturation),
        Codec.BOOL.optionalFieldOf("meat", false).forGetter(FoodComponent::meat),
        Codec.BOOL.optionalFieldOf("always_edible", false).forGetter(FoodComponent::alwaysEdible),
        Codec.BOOL.optionalFieldOf("snack", false).forGetter(FoodComponent::snack),
        FoodEffect.CODEC.optionalFieldOf("effect").forGetter(FoodComponent::effect),
        Codec.list(FoodEffect.CODEC).optionalFieldOf("effects").forGetter(FoodComponent::effects)
    ).apply(i, FoodComponent::new));

    public FoodProperties build(@Nullable LivingEntity eater) {
        FoodProperties.Builder b = new FoodProperties.Builder()
            .nutrition(hunger).saturationMod(saturation);
        if (meat) b.meat();
        if (alwaysEdible) b.alwaysEat();
        if (snack) b.fast();
        effect.ifPresent(entry -> b.effect(entry.spec().resolve(eater), entry.chance()));
        effects.ifPresent(list -> list.forEach(entry -> b.effect(entry.spec().resolve(eater), entry.chance())));
        return b.build();
    }

    public List<Pair<MobEffectInstance, Float>> allEffectsWithProbability(@Nullable LivingEntity eater) {
        java.util.ArrayList<Pair<MobEffectInstance, Float>> out = new java.util.ArrayList<>();
        effect.ifPresent(entry -> out.add(Pair.of(entry.spec().resolve(eater), entry.chance())));
        effects.ifPresent(list -> list.forEach(entry -> out.add(Pair.of(entry.spec().resolve(eater), entry.chance()))));
        return out;
    }
}
