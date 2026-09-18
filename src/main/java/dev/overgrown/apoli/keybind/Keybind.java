package dev.overgrown.apoli.keybind;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record Keybind(ResourceLocation id, String key, String category, Optional<Component> name) {
    public static final String DEFAULT_CATEGORY = "key.categories.apoli";

    public static final Codec<Keybind> CODEC_NO_ID = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("key").forGetter(Keybind::key),
        Codec.STRING.optionalFieldOf("category", DEFAULT_CATEGORY).forGetter(Keybind::category),
        TextComponent.CODEC.optionalFieldOf("name").forGetter(Keybind::name)
    ).apply(i, (k, c, n) -> new Keybind(null, k, c, n)));

    public Keybind withId(ResourceLocation id) {
        return new Keybind(id, key, category, name);
    }

    public String translationKey() {
        return "key." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static String encodeName(Component component) {
        String plain = component.getString();
        if (Component.literal(plain).equals(component)) return plain;
        return TextComponent.CODEC.encodeStart(JsonOps.INSTANCE, component)
            .result()
            .map(JsonElement::toString)
            .orElse(plain);
    }

    public static Component decodeName(String encoded) {
        if (encoded.isEmpty()) return Component.empty();
        char first = encoded.charAt(0);
        if (first == '{' || first == '[' || first == '"') {
            try {
                Optional<Component> parsed = TextComponent.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(encoded))
                    .result();
                if (parsed.isPresent()) return parsed.get();
            } catch (RuntimeException ignored) {
            }
        }
        return Component.literal(encoded);
    }
}
