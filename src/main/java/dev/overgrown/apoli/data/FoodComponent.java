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
    Optional<EffectSpec> effect,
    Optional<List<EffectSpec>> effects
) {
    public static final Codec<FoodComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("hunger").forGetter(FoodComponent::hunger),
        Codec.FLOAT.fieldOf("saturation").forGetter(FoodComponent::saturation),
        Codec.BOOL.optionalFieldOf("meat", false).forGetter(FoodComponent::meat),
        Codec.BOOL.optionalFieldOf("always_edible", false).forGetter(FoodComponent::alwaysEdible),
        Codec.BOOL.optionalFieldOf("snack", false).forGetter(FoodComponent::snack),
        EffectSpec.CODEC.optionalFieldOf("effect").forGetter(FoodComponent::effect),
        Codec.list(EffectSpec.CODEC).optionalFieldOf("effects").forGetter(FoodComponent::effects)
    ).apply(i, FoodComponent::new));

    public FoodProperties build(@Nullable LivingEntity eater) {
        FoodProperties.Builder b = new FoodProperties.Builder()
            .nutrition(hunger).saturationModifier(saturation);
        if (alwaysEdible) b.alwaysEdible();
        if (snack) b.fast();
        effect.ifPresent(spec -> b.effect(spec.resolve(eater), 1.0f));
        effects.ifPresent(list -> list.forEach(spec -> b.effect(spec.resolve(eater), 1.0f)));
        return b.build();
    }

    public List<Pair<MobEffectInstance, Float>> allEffectsWithProbability(@Nullable LivingEntity eater) {
        java.util.ArrayList<Pair<MobEffectInstance, Float>> out = new java.util.ArrayList<>();
        effect.ifPresent(spec -> out.add(Pair.of(spec.resolve(eater), 1.0f)));
        effects.ifPresent(list -> list.forEach(spec -> out.add(Pair.of(spec.resolve(eater), 1.0f))));
        return out;
    }
}
