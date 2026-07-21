package dev.overgrown.apoli.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.overgrown.apoli.data.Key;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class KeyPressWatcher {
    private static volatile Set<String> watched = Set.of();
    private static volatile Set<String> heldSnapshot = Set.of();
    private static Set<String> lastSent = Set.of();
    private static Consumer<List<String>> sender = keys -> {};

    private KeyPressWatcher() {}

    public static void setSender(Consumer<List<String>> next) {
        sender = next;
    }

    public static void reset() {
        watched = Set.of();
        heldSnapshot = Set.of();
        lastSent = Set.of();
    }

    public static boolean isLocalHeld(String key) {
        return heldSnapshot.contains(key);
    }

    public static void rebuild(Map<ResourceLocation, String> rawPowers) {
        Set<String> next = new HashSet<>();
        for (String json : rawPowers.values()) {
            try {
                collect(JsonParser.parseString(json), next);
            } catch (RuntimeException ignored) {
            }
        }
        watched = Set.copyOf(next);
    }

    public static void tick() {
        Set<String> keys = watched;
        if (keys.isEmpty()) {
            if (!heldSnapshot.isEmpty()) heldSnapshot = Set.of();
            if (!lastSent.isEmpty()) {
                lastSent = Set.of();
                sender.accept(List.of());
            }
            return;
        }
        Set<String> held = new HashSet<>();
        for (String key : keys) {
            KeyMapping mapping = ApoliKeyMappings.resolve(key);
            if (mapping != null && ApoliKeyMappings.isDown(mapping)) held.add(key);
        }
        heldSnapshot = held;
        if (!held.equals(lastSent)) {
            lastSent = held;
            sender.accept(new ArrayList<>(held));
        }
    }

    private static void collect(JsonElement element, Set<String> out) {
        if (element == null) return;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement typeElement = object.get("type");
            if (typeElement != null && typeElement.isJsonPrimitive()) {
                String typeStr = typeElement.getAsString();
                if (isKeyPressedType(typeStr)) {
                    out.add(extractKey(object));
                } else if (isKeySequenceType(typeStr)) {
                    collectSequenceKeys(object, out);
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collect(entry.getValue(), out);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collect(child, out);
            }
        }
    }

    private static boolean isKeyPressedType(String type) {
        ResourceLocation id = ResourceLocation.tryParse(type);
        String path = id != null ? id.getPath() : type;
        return path.equals("key_pressed") || path.equals("key_held") || path.equals("held_key");
    }

    private static boolean isKeySequenceType(String type) {
        ResourceLocation id = ResourceLocation.tryParse(type);
        String path = id != null ? id.getPath() : type;
        return path.equals("action_on_key_sequence");
    }

    private static void collectSequenceKeys(JsonObject powerObject, Set<String> out) {
        JsonElement keys = powerObject.get("keys");
        if (keys == null || !keys.isJsonArray()) return;
        for (JsonElement entry : keys.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                out.add(entry.getAsString());
            } else if (entry.isJsonObject()) {
                JsonElement key = entry.getAsJsonObject().get("key");
                if (key != null && key.isJsonPrimitive()) out.add(key.getAsString());
            }
        }
    }

    private static String extractKey(JsonObject conditionObject) {
        JsonElement keyElement = conditionObject.get("key");
        if (keyElement == null) return Key.PRIMARY_ACTIVE;
        if (keyElement.isJsonPrimitive()) return keyElement.getAsString();
        if (keyElement.isJsonObject()) {
            JsonElement inner = keyElement.getAsJsonObject().get("key");
            if (inner != null && inner.isJsonPrimitive()) return inner.getAsString();
        }
        return Key.PRIMARY_ACTIVE;
    }
}
