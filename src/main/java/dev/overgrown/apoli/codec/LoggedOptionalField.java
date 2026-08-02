package dev.overgrown.apoli.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.stream.Stream;

public final class LoggedOptionalField<A> extends MapCodec<Optional<A>> {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private final String name;
    private final Codec<A> elementCodec;

    private LoggedOptionalField(String name, Codec<A> elementCodec) {
        this.name = name;
        this.elementCodec = elementCodec;
    }

    public static <A> MapCodec<Optional<A>> of(String name, Codec<A> elementCodec) {
        return new LoggedOptionalField<>(name, elementCodec);
    }

    public static <A> MapCodec<A> of(String name, Codec<A> elementCodec, A fallback) {
        return new LoggedOptionalField<>(name, elementCodec)
            .xmap(o -> o.orElse(fallback), Optional::ofNullable);
    }

    public static void setContext(ResourceLocation id) {
        CONTEXT.set(id == null ? null : id.toString());
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString(name));
    }

    @Override
    public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
        T value = input.get(name);
        if (value == null) return DataResult.success(Optional.empty());
        DataResult<A> parsed = elementCodec.parse(ops, value);
        Optional<A> result = parsed.result();
        if (result.isPresent()) return DataResult.success(result);
        String where = CONTEXT.get();
        Apoli.LOGGER.warn("[Apoli] Ignoring the '{}' field{} — it is present but failed to parse, so it was dropped and everything else loaded. {}",
            name, where == null ? "" : " of " + where,
            parsed.error().map(e -> e.message()).orElse("unknown error"));
        return DataResult.success(Optional.empty());
    }

    @Override
    public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return input.isPresent() ? prefix.add(name, elementCodec.encodeStart(ops, input.get())) : prefix;
    }
}
