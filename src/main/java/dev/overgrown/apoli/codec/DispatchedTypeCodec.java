package dev.overgrown.apoli.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class DispatchedTypeCodec {
    private DispatchedTypeCodec() {}

    /** Resolves a JSON {@code type} field to the canonical id + its config codec. */
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
                    return DataResult.<MapCodec<? extends W>>error(() -> "Unknown " + groupName + " type: " + id);
                }
                ResourceLocation canonical = resolution.canonical();
                @SuppressWarnings({"rawtypes", "unchecked"})
                MapCodec<Object> objCodec = (MapCodec) resolution.codec();
                MapCodec<W> wrapped = objCodec.xmap(
                    cfg -> ctor.apply(canonical, cfg),
                    (Object w) -> configGetter.apply((W) w)
                );
                return DataResult.<MapCodec<? extends W>>success(wrapped);
            }
        );
    }
}
