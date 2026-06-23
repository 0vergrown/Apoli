package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;

import java.util.List;
import java.util.Optional;

public record FoodComponent(
    int hunger,
    float saturation,
    boolean meat,
    boolean alwaysEdible,
    boolean snack,
    Optional<MobEffectInstance> effect,
    Optional<List<MobEffectInstance>> effects
) {
    public static final Codec<FoodComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("hunger").forGetter(FoodComponent::hunger),
        Codec.FLOAT.fieldOf("saturation").forGetter(FoodComponent::saturation),
        Codec.BOOL.optionalFieldOf("meat", false).forGetter(FoodComponent::meat),
        Codec.BOOL.optionalFieldOf("always_edible", false).forGetter(FoodComponent::alwaysEdible),
        Codec.BOOL.optionalFieldOf("snack", false).forGetter(FoodComponent::snack),
        StatusEffectInstanceCodec.CODEC.optionalFieldOf("effect").forGetter(FoodComponent::effect),
        Codec.list(StatusEffectInstanceCodec.CODEC).optionalFieldOf("effects").forGetter(FoodComponent::effects)
    ).apply(i, FoodComponent::new));

    public FoodProperties build() {
        FoodProperties.Builder b = new FoodProperties.Builder()
            .nutrition(hunger).saturationMod(saturation);
        if (meat) b.meat();
        if (alwaysEdible) b.alwaysEat();
        if (snack) b.fast();
        effect.ifPresent(e -> b.effect(e, 1.0f));
        effects.ifPresent(list -> list.forEach(e -> b.effect(e, 1.0f)));
        return b.build();
    }

    public List<Pair<MobEffectInstance, Float>> allEffectsWithProbability() {
        java.util.ArrayList<Pair<MobEffectInstance, Float>> out = new java.util.ArrayList<>();
        effect.ifPresent(e -> out.add(Pair.of(e, 1.0f)));
        effects.ifPresent(list -> list.forEach(e -> out.add(Pair.of(e, 1.0f))));
        return out;
    }
}
