package dev.overgrown.apoli.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class ClientPowerState {
    private static final Map<ResourceLocation, Integer> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<ResourceLocation, Set<ResourceLocation>>> ENTITY_POWERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<ResourceLocation, Integer>> ENTITY_AUX = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<ResourceLocation>> ENTITY_SUPPRESSED = new ConcurrentHashMap<>();

    private ClientPowerState() {}

    public static void applyPowersSync(SyncPowersS2C payload) {
        Map<ResourceLocation, Power> decoded = new HashMap<>();
        payload.rawPowers().forEach((id, json) -> {
            JsonElement element = JsonParser.parseString(json);
            Power.CODEC.parse(JsonOps.INSTANCE, element).result().ifPresent(p -> decoded.put(id, p));
        });
        ApoliPowers.replaceAll(decoded);
    }

    public static void applyEntityPowersSync(SyncEntityPowersS2C payload) {
        ENTITY_POWERS.put(payload.entityId(), Map.copyOf(payload.powersBySource()));
        ENTITY_AUX.put(payload.entityId(), Map.copyOf(payload.auxInt()));
        ENTITY_SUPPRESSED.put(payload.entityId(), Set.copyOf(payload.suppressed()));
    }

    public static void removeEntity(int entityId) {
        ENTITY_POWERS.remove(entityId);
        ENTITY_AUX.remove(entityId);
        ENTITY_SUPPRESSED.remove(entityId);
    }

    public static void clear() {
        ENTITY_POWERS.clear();
        ENTITY_AUX.clear();
        ENTITY_SUPPRESSED.clear();
        COOLDOWNS.clear();
    }

    public static Set<ResourceLocation> suppressedFor(int entityId) {
        return ENTITY_SUPPRESSED.getOrDefault(entityId, Set.of());
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> powersFor(int entityId) {
        return ENTITY_POWERS.getOrDefault(entityId, Map.of());
    }

    public static Map<ResourceLocation, Integer> auxFor(int entityId) {
        return ENTITY_AUX.getOrDefault(entityId, Map.of());
    }

    private static int localId() {
        var p = Minecraft.getInstance().player;
        return p == null ? -1 : p.getId();
    }

    public static Set<ResourceLocation> localPowers() {
        return powersFor(localId()).keySet();
    }

    public static Set<ResourceLocation> localSourcesOf(ResourceLocation power) {
        Set<ResourceLocation> s = powersFor(localId()).get(power);
        return s == null ? Set.of() : s;
    }

    public static Set<ResourceLocation> localAllSources() {
        HashSet<ResourceLocation> out = new HashSet<>();
        for (Set<ResourceLocation> s : powersFor(localId()).values()) out.addAll(s);
        return out;
    }

    public static Map<ResourceLocation, Integer> localAuxInt() {
        return auxFor(localId());
    }

    public static OptionalInt getAuxInt(ResourceLocation powerId) {
        Integer v = auxFor(localId()).get(powerId);
        return v == null ? OptionalInt.empty() : OptionalInt.of(v);
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
