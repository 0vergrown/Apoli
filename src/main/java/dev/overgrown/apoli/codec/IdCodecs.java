package dev.overgrown.apoli.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public final class IdCodecs {

    public static final Codec<ResourceLocation> ID =
        Codec.STRING.comapFlatMap(IdCodecs::readId, ResourceLocation::toString).stable();

    public static final Codec<ResourceLocation> TAG =
        Codec.STRING.comapFlatMap(IdCodecs::readTag, id -> "#" + id).stable();

    private IdCodecs() {}

    public static <T> Codec<TagKey<T>> tagKey(ResourceKey<? extends Registry<T>> registry) {
        return Codec.STRING.comapFlatMap(
            raw -> readTag(raw).map(id -> TagKey.create(registry, id)),
            tag -> "#" + tag.location()).stable();
    }

    public static DataResult<ResourceLocation> readId(String raw) {
        if (raw.startsWith("#")) {
            return DataResult.error(() -> "'" + raw + "' is a tag, and this field takes a single id. "
                + "Drop the '#', or use the matching in_tag condition.");
        }
        return parse(raw);
    }

    public static DataResult<ResourceLocation> readTag(String raw) {
        return parse(raw.startsWith("#") ? raw.substring(1) : raw);
    }

    private static DataResult<ResourceLocation> parse(String raw) {
        if (raw.indexOf('*') >= 0) {
            return DataResult.error(() -> "'" + raw + "' still contains a '*' wildcard. "
                + "'*' stands for the namespace of the file it is written in, and '*:*' for that file's full id, "
                + "so it only resolves inside a power, origin, layer, global power set or skill tree file.");
        }
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null) {
            return DataResult.error(() -> "'" + raw + "' is not a valid id. "
                + "Ids look like 'namespace:path', where the namespace is [a-z0-9_.-] "
                + "and the path may also contain '/'.");
        }
        return DataResult.success(parsed);
    }
}
