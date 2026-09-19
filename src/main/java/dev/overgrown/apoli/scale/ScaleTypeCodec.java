package dev.overgrown.apoli.scale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ScaleTypeCodec {
    private ScaleTypeCodec() {}

    public static final Codec<ScaleType> CODEC = IdCodecs.ID.comapFlatMap(
        id -> {
            ScaleType type = resolve(id);
            return type != null
                ? DataResult.success(type)
                : DataResult.error(() -> "Unknown scale type: " + id);
        },
        ScaleType::id);

    public static final Codec<List<ScaleType>> LIST_OR_SINGLE = Codec.either(CODEC, CODEC.listOf()).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1
            ? com.mojang.datafixers.util.Either.left(list.get(0))
            : com.mojang.datafixers.util.Either.right(list));

    public static @Nullable ScaleType resolve(ResourceLocation id) {
        ScaleType type = ScaleTypes.get(id);
        if (type != null) return type;
        String namespace = id.getNamespace();
        if (namespace.equals("minecraft") || namespace.equals("pehkui") || namespace.equals("origins")) {
            return ScaleTypes.get(Apoli.id(id.getPath()));
        }
        return null;
    }
}
