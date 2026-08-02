package dev.overgrown.apoli.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.overgrown.apoli.data.PlayerModelType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerModelTypes {

    private static final Map<UUID, PlayerModelType> KNOWN = new ConcurrentHashMap<>();

    private static final int DEFAULT_SKIN_COUNT = 18;
    private static final int DEFAULT_SLIM_COUNT = 9;

    private PlayerModelTypes() {}

    public static void set(UUID player, PlayerModelType type) {
        KNOWN.put(player, type);
    }

    public static void resolveFrom(UUID player, GameProfile profile) {
        PlayerModelType derived = fromProfile(profile);
        KNOWN.putIfAbsent(player, derived != null ? derived : defaultFor(player));
    }

    public static void remove(UUID player) {
        KNOWN.remove(player);
    }

    public static void clear() {
        KNOWN.clear();
    }

    public static PlayerModelType of(Entity entity) {
        if (!(entity instanceof Player)) return PlayerModelType.WIDE;
        UUID id = entity.getUUID();
        PlayerModelType known = KNOWN.get(id);
        return known != null ? known : defaultFor(id);
    }

    public static PlayerModelType defaultFor(UUID player) {
        return Math.floorMod(player.hashCode(), DEFAULT_SKIN_COUNT) < DEFAULT_SLIM_COUNT
            ? PlayerModelType.SLIM
            : PlayerModelType.WIDE;
    }

    public static @Nullable PlayerModelType fromProfile(@Nullable GameProfile profile) {
        if (profile == null) return null;
        for (Property property : profile.getProperties().get("textures")) {
            String encoded = property.getValue();
            if (encoded == null || encoded.isEmpty()) continue;
            try {
                String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                JsonElement root = JsonParser.parseString(json);
                if (!root.isJsonObject()) continue;
                JsonElement textures = root.getAsJsonObject().get("textures");
                if (textures == null || !textures.isJsonObject()) continue;
                JsonElement skin = textures.getAsJsonObject().get("SKIN");
                if (skin == null || !skin.isJsonObject()) continue;
                JsonElement metadata = skin.getAsJsonObject().get("metadata");
                if (metadata == null || !metadata.isJsonObject()) return PlayerModelType.WIDE;
                JsonElement model = metadata.getAsJsonObject().get("model");
                return model != null && "slim".equalsIgnoreCase(model.getAsString())
                    ? PlayerModelType.SLIM
                    : PlayerModelType.WIDE;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
