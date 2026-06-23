package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LazyCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public record AttributeModifier(
    AttributeModifierOperation operation,
    Expression value,
    Optional<ResourceLocation> attribute,
    Optional<String> name,
    Optional<ResourceLocation> resource,
    Optional<AttributeModifier> nested
) {
    public static final Codec<AttributeModifier> CODEC;

    static {
        @SuppressWarnings("unchecked")
        Codec<AttributeModifier>[] ref = (Codec<AttributeModifier>[]) new Codec[1];
        MapCodec<AttributeModifier> rawMap = RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifierOperation.CODEC.fieldOf("operation").forGetter(AttributeModifier::operation),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("value", Expression.constant(0.0)).forGetter(AttributeModifier::value),
            ResourceLocation.CODEC.optionalFieldOf("attribute").forGetter(AttributeModifier::attribute),
            Codec.STRING.optionalFieldOf("name").forGetter(AttributeModifier::name),
            ResourceLocation.CODEC.optionalFieldOf("resource").forGetter(AttributeModifier::resource),
            new LazyCodec<AttributeModifier>(() -> ref[0]).optionalFieldOf("modifier").forGetter(AttributeModifier::nested)
        ).apply(i, AttributeModifier::new));
        Codec<AttributeModifier> built = AliasingMapCodec.wrap(rawMap, Map.of("amount", "value")).codec();
        ref[0] = built;
        CODEC = built;
    }

    public static final Codec<java.util.List<AttributeModifier>> LIST_OR_SINGLE = Codec.either(
        CODEC, Codec.list(CODEC)
    ).xmap(
        either -> either.map(java.util.List::of, java.util.function.Function.identity()),
        list -> list.size() == 1
            ? com.mojang.datafixers.util.Either.left(list.get(0))
            : com.mojang.datafixers.util.Either.right(list)
    );

    public double resolveInput(
        Map<String, Double> variables,
        Map<String, Double> resources
    ) {
        double base;
        if (resource.isPresent()) {
            base = resources.getOrDefault(resource.get().toString(), 0.0);
        } else {
            base = value.eval(variables, resources);
        }
        if (nested.isPresent()) {
            double nestedInput = nested.get().resolveInput(variables, resources);
            return nested.get().operation.applySingle(base, nestedInput);
        }
        return base;
    }

    public double applyToValue(
        double currentValue,
        Map<String, Double> variables,
        Map<String, Double> resources
    ) {
        double input = resolveInput(variables, resources);
        return operation.applySingle(currentValue, input);
    }
}
