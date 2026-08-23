package dev.overgrown.apoli.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.data.Key;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

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
                collect(new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(json)), next);
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

    private static <T> void collect(Dynamic<T> data, Set<String> out) {
        Map<Dynamic<T>, Dynamic<T>> fields = data.getMapValues().result().orElse(null);
        if (fields != null) {
            String type = data.get("type").asString().result().orElse(null);
            if (type != null) {
                if (isKeyPressedType(type)) {
                    out.add(extractKey(data));
                } else if (isKeySequenceType(type)) {
                    collectSequenceKeys(data, out);
                }
            }
            for (Dynamic<T> value : fields.values()) collect(value, out);
            return;
        }
        data.asStreamOpt().result().ifPresent(stream -> stream.forEach(child -> collect(child, out)));
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

    private static <T> void collectSequenceKeys(Dynamic<T> power, Set<String> out) {
        power.get("keys").asStreamOpt().result().ifPresent(stream -> stream.forEach(entry -> {
            String direct = entry.asString().result().orElse(null);
            if (direct != null) {
                out.add(direct);
                return;
            }
            entry.get("key").asString().result().ifPresent(out::add);
        }));
    }

    private static <T> String extractKey(Dynamic<T> condition) {
        Dynamic<T> key = condition.get("key").result().orElse(null);
        if (key == null) return Key.PRIMARY_ACTIVE;
        String direct = key.asString().result().orElse(null);
        if (direct != null) return direct;
        return key.get("key").asString().result().orElse(Key.PRIMARY_ACTIVE);
    }
}
