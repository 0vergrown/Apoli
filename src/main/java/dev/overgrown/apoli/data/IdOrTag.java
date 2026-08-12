package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

public final class IdOrTag<T> {
    private final ResourceLocation id;
    private final @Nullable TagKey<T> tag;

    private IdOrTag(ResourceLocation id, @Nullable TagKey<T> tag) {
        this.id = id;
        this.tag = tag;
    }

    public static <T> IdOrTag<T> id(ResourceLocation id) {
        return new IdOrTag<>(id, null);
    }

    public static <T> IdOrTag<T> tag(ResourceKey<? extends Registry<T>> registry, ResourceLocation id) {
        return new IdOrTag<>(id, TagKey.create(registry, id));
    }

    public static <T> Codec<IdOrTag<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        return Codec.STRING.comapFlatMap(raw -> read(raw, registry), IdOrTag::asString).stable();
    }

    private static <T> DataResult<IdOrTag<T>> read(String raw, ResourceKey<? extends Registry<T>> registry) {
        boolean tagged = raw.startsWith("#");
        DataResult<ResourceLocation> parsed = tagged ? IdCodecs.readTag(raw) : IdCodecs.readId(raw);
        return parsed.map(id -> new IdOrTag<>(id, tagged ? TagKey.create(registry, id) : null));
    }

    public boolean matches(Holder<T> holder) {
        return tag != null ? holder.is(tag) : holder.is(id);
    }

    public boolean isTag() {
        return tag != null;
    }

    public ResourceLocation id() {
        return id;
    }

    public @Nullable TagKey<T> tag() {
        return tag;
    }

    public String asString() {
        return tag != null ? "#" + id : id.toString();
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IdOrTag<?> o)) return false;
        return id.equals(o.id) && (tag == null) == (o.tag == null);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + (tag == null ? 0 : 1);
    }
}
