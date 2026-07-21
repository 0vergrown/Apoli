package dev.overgrown.apoli.keybind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Keybind(ResourceLocation id, String key, String category, Optional<String> name) {
    public static final String DEFAULT_CATEGORY = "key.categories.apoli";

    public static final Codec<Keybind> CODEC_NO_ID = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("key").forGetter(Keybind::key),
        Codec.STRING.optionalFieldOf("category", DEFAULT_CATEGORY).forGetter(Keybind::category),
        Codec.STRING.optionalFieldOf("name").forGetter(Keybind::name)
    ).apply(i, (k, c, n) -> new Keybind(null, k, c, n)));

    public Keybind withId(ResourceLocation id) {
        return new Keybind(id, key, category, name);
    }

    public String translationKey() {
        return "key." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
