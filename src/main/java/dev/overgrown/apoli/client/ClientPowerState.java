package dev.overgrown.apoli.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPowerState {
    private static final Map<ResourceLocation, Integer> COOLDOWNS = new ConcurrentHashMap<>();
    private static volatile Map<ResourceLocation, Set<ResourceLocation>> LOCAL_POWERS = Map.of();

    private ClientPowerState() {}

    public static void applyPowersSync(SyncPowersS2C payload) {
        Map<ResourceLocation, Power> decoded = new HashMap<>();
        payload.rawPowers().forEach((id, json) -> {
            JsonElement element = JsonParser.parseString(json);
            Power.CODEC.parse(JsonOps.INSTANCE, element).result().ifPresent(p -> decoded.put(id, p));
        });
        ApoliPowers.replaceAll(decoded);
    }

    public static void applyEntityPowersSync(SyncEntityPowersS2C payload, int localPlayerId) {
        if (payload.entityId() != localPlayerId) return;
        LOCAL_POWERS = Map.copyOf(payload.powersBySource());
    }

    public static Set<ResourceLocation> localPowers() {
        return LOCAL_POWERS.keySet();
    }

    public static void setCooldown(ResourceLocation power, int ticks) {
        if (ticks <= 0) COOLDOWNS.remove(power);
        else COOLDOWNS.put(power, ticks);
    }

    public static int getCooldown(ResourceLocation power) {
        return COOLDOWNS.getOrDefault(power, 0);
    }

    public static void tickCooldowns() {
        COOLDOWNS.replaceAll((k, v) -> Math.max(0, v - 1));
        COOLDOWNS.values().removeIf(v -> v == 0);
    }

    public static @Nullable Power getLocalPower(ResourceLocation id) {
        return ApoliPowers.get(id);
    }
}
