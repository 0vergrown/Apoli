package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record FoodComponent(
    int hunger,
    float saturation,
    boolean meat,
    boolean alwaysEdible,
    boolean snack,
    Optional<Float> eatSeconds,
    Optional<FoodEffect> effect,
    Optional<List<FoodEffect>> effects
) {
    public static final float DEFAULT_EAT_SECONDS = 1.6F;
    public static final float SNACK_EAT_SECONDS = 0.8F;

    public record FoodEffect(EffectSpec spec, float chance) {
        private static final Codec<FoodEffect> FLAT = RecordCodecBuilder.create(i -> i.group(
            EffectSpec.MAP_CODEC.forGetter(FoodEffect::spec),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(FoodEffect::chance)
        ).apply(i, FoodEffect::new));

        private static final Codec<FoodEffect> NESTED = RecordCodecBuilder.create(i -> i.group(
            EffectSpec.CODEC.fieldOf("effect").forGetter(FoodEffect::spec),
            Codec.FLOAT.optionalFieldOf("probability", 1.0F).forGetter(FoodEffect::chance)
        ).apply(i, FoodEffect::new));

        public static final Codec<FoodEffect> CODEC = Codec.either(FLAT, NESTED).xmap(
            either -> either.map(Function.identity(), Function.identity()),
            Either::left
        );
    }

    private static final MapCodec<FoodComponent> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.fieldOf("hunger").forGetter(FoodComponent::hunger),
        Codec.FLOAT.fieldOf("saturation").forGetter(FoodComponent::saturation),
        Codec.BOOL.optionalFieldOf("meat", false).forGetter(FoodComponent::meat),
        Codec.BOOL.optionalFieldOf("always_edible", false).forGetter(FoodComponent::alwaysEdible),
        Codec.BOOL.optionalFieldOf("snack", false).forGetter(FoodComponent::snack),
        Codec.FLOAT.optionalFieldOf("eat_seconds").forGetter(FoodComponent::eatSeconds),
        FoodEffect.CODEC.optionalFieldOf("effect").forGetter(FoodComponent::effect),
        Codec.list(FoodEffect.CODEC).optionalFieldOf("effects").forGetter(FoodComponent::effects)
    ).apply(i, FoodComponent::new));

    public static final Codec<FoodComponent> CODEC = AliasingMapCodec.wrap(INNER, Map.of(
        "nutrition", "hunger",
        "can_always_eat", "always_edible"
    )).codec();

    public float resolvedEatSeconds() {
        return eatSeconds.orElse(snack ? SNACK_EAT_SECONDS : DEFAULT_EAT_SECONDS);
    }

    public int eatDurationTicks() {
        return Math.max(1, (int) (resolvedEatSeconds() * 20.0F));
    }

    @SuppressWarnings("deprecation")
    public FoodProperties build(@Nullable LivingEntity eater) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
            .nutrition(hunger)
            .saturationModifier(saturation);
        if (alwaysEdible) builder.alwaysEdible();
        effect.ifPresent(entry -> builder.effect(entry.spec().resolve(eater), entry.chance()));
        effects.ifPresent(list -> list.forEach(entry -> builder.effect(entry.spec().resolve(eater), entry.chance())));
        FoodProperties built = builder.build();
        return new FoodProperties(hunger, saturation, alwaysEdible, resolvedEatSeconds(),
            built.usingConvertsTo(), built.effects());
    }

}
