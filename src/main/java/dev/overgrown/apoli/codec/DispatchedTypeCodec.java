package dev.overgrown.apoli.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public final class DispatchedTypeCodec {
    private DispatchedTypeCodec() {}

    @FunctionalInterface
    public interface InvertibleCtor<W> {
        W apply(ResourceLocation typeId, Object config, boolean inverted);
    }

    @FunctionalInterface
    public interface CodecLookup {
        @Nullable Resolution lookup(ResourceLocation id);

        record Resolution(ResourceLocation canonical, MapCodec<?> codec) {}
    }

    public static <W> Codec<W> create(
        String groupName,
        CodecLookup lookup,
        BiFunction<ResourceLocation, Object, W> ctor,
        Function<W, ResourceLocation> typeIdGetter,
        Function<W, Object> configGetter
    ) {
        return ResourceLocation.CODEC.partialDispatch(
            "type",
            w -> DataResult.success(typeIdGetter.apply(w)),
            id -> {
                CodecLookup.Resolution resolution = lookup.lookup(id);
                if (resolution == null) {
                    return DataResult.<Codec<? extends W>>error(() -> "Unknown " + groupName + " type: " + id);
                }
                ResourceLocation canonical = resolution.canonical();
                @SuppressWarnings({"rawtypes", "unchecked"})
                MapCodec<Object> objCodec = (MapCodec) resolution.codec();
                Codec<W> wrapped = objCodec.xmap(
                    cfg -> ctor.apply(canonical, cfg),
                    (Object w) -> configGetter.apply((W) w)
                ).codec();
                return DataResult.<Codec<? extends W>>success(wrapped);
            }
        );
    }

    public static <W> Codec<W> createInvertible(
        String groupName,
        CodecLookup lookup,
        InvertibleCtor<W> ctor,
        Function<W, ResourceLocation> typeIdGetter,
        Function<W, Object> configGetter,
        Function<W, Boolean> invertedGetter
    ) {
        return ResourceLocation.CODEC.partialDispatch(
            "type",
            w -> DataResult.success(typeIdGetter.apply(w)),
            id -> {
                CodecLookup.Resolution resolution = lookup.lookup(id);
                if (resolution == null) {
                    return DataResult.<Codec<? extends W>>error(() -> "Unknown " + groupName + " type: " + id);
                }
                ResourceLocation canonical = resolution.canonical();
                @SuppressWarnings({"rawtypes", "unchecked"})
                MapCodec<Object> objCodec = (MapCodec) resolution.codec();
                MapCodec<W> wrappedMap = new InvertibleMapCodec<>(canonical, objCodec, ctor, configGetter, invertedGetter);
                return DataResult.<Codec<? extends W>>success(wrappedMap.codec());
            }
        );
    }

    private static final class InvertibleMapCodec<W> extends MapCodec<W> {
        private static final MapCodec<Boolean> INVERTED_FIELD = Codec.BOOL.optionalFieldOf("inverted", false);
        private final ResourceLocation canonical;
        private final MapCodec<Object> configCodec;
        private final InvertibleCtor<W> ctor;
        private final Function<W, Object> configGetter;
        private final Function<W, Boolean> invertedGetter;

        InvertibleMapCodec(ResourceLocation canonical, MapCodec<Object> configCodec, InvertibleCtor<W> ctor,
                           Function<W, Object> configGetter, Function<W, Boolean> invertedGetter) {
            this.canonical = canonical;
            this.configCodec = configCodec;
            this.ctor = ctor;
            this.configGetter = configGetter;
            this.invertedGetter = invertedGetter;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(INVERTED_FIELD.keys(ops), configCodec.keys(ops));
        }

        @Override
        public <T> DataResult<W> decode(DynamicOps<T> ops, MapLike<T> input) {
            DataResult<Boolean> invertedResult = INVERTED_FIELD.decode(ops, input);
            DataResult<Object> configResult = configCodec.decode(ops, input);
            return invertedResult.flatMap(inverted -> configResult.map(cfg -> ctor.apply(canonical, cfg, inverted)));
        }

        @Override
        public <T> RecordBuilder<T> encode(W input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            prefix = INVERTED_FIELD.encode(invertedGetter.apply(input), ops, prefix);
            prefix = configCodec.encode(configGetter.apply(input), ops, prefix);
            return prefix;
        }
    }
}
