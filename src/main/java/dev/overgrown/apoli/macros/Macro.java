package dev.overgrown.apoli.macros;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.Placeholders;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record Macro(ResourceLocation id, ResourceLocation scope, List<String> parameters, Dynamic<?> value) {

    private record Definition(Optional<List<String>> parameters, Dynamic<?> value) {}

    private static final Codec<Definition> DEFINITION = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("parameters").forGetter(Definition::parameters),
        Codec.PASSTHROUGH.fieldOf("value").forGetter(Definition::value)
    ).apply(i, Definition::new));

    public static <T> DataResult<Macro> parse(ResourceLocation id, ResourceLocation scope, Dynamic<T> definition) {
        return DEFINITION.parse(definition).flatMap(body ->
            Placeholders.parameters("macro " + id, body.value(), body.parameters())
                .map(parameters -> new Macro(id, scope, parameters, body.value())));
    }
}
