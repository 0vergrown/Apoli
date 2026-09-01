package dev.overgrown.apoli.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncPowersChunkS2C;
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
        KeyPressWatcher.rebuild(payload.rawPowers());
        if (Minecraft.getInstance().hasSingleplayerServer()) return;

        Map<ResourceLocation, Power> decoded = new HashMap<>();
        payload.rawPowers().forEach((id, json) -> {
            JsonElement element = JsonParser.parseString(json);
            Power.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Apoli.LOGGER.error("[Apoli] Client failed to parse power {}: {}", id, error))
                .ifPresent(p -> decoded.put(id, p));
        });
        ApoliPowers.replaceAll(decoded);
    }

    private static final java.util.List<byte[]> PENDING_SLICES = new java.util.ArrayList<>();
    private static int expectedSlice;

    public static void applyPowersChunk(SyncPowersChunkS2C payload) {
        if (payload.index() == 0) {
            PENDING_SLICES.clear();
            expectedSlice = 0;
        }
        if (payload.index() != expectedSlice) {
            PENDING_SLICES.clear();
            expectedSlice = 0;
            return;
        }
        PENDING_SLICES.add(payload.slice());
        expectedSlice++;
        if (expectedSlice < payload.total()) {
            return;
        }
        int size = 0;
        for (byte[] s : PENDING_SLICES) {
            size += s.length;
        }
        byte[] gz = new byte[size];
        int at = 0;
        for (byte[] s : PENDING_SLICES) {
            System.arraycopy(s, 0, gz, at, s.length);
            at += s.length;
        }
        PENDING_SLICES.clear();
        expectedSlice = 0;
        try {
            applyPowersSync(new SyncPowersS2C(SyncPowersChunkS2C.decodeBlob(gz)));
        } catch (java.io.IOException e) {
            Apoli.LOGGER.error("[Apoli] Failed to decode chunked power sync", e);
        }
    }

    public static void applyEntityPowersSync(SyncEntityPowersS2C payload) {
        ENTITY_POWERS.put(payload.entityId(), Map.copyOf(payload.powersBySource()));
        ENTITY_AUX.put(payload.entityId(), Map.copyOf(payload.auxInt()));
        ENTITY_SUPPRESSED.put(payload.entityId(), Set.copyOf(payload.suppressed()));
    }

    public static void applyAuxInts(dev.overgrown.apoli.network.payload.SyncAuxIntsS2C payload) {
        if (!ENTITY_POWERS.containsKey(payload.entityId())) return;
        ENTITY_AUX.put(payload.entityId(), Map.copyOf(payload.auxInt()));
    }

    public static void removeEntity(int entityId) {
        ENTITY_POWERS.remove(entityId);
        ENTITY_AUX.remove(entityId);
        ENTITY_SUPPRESSED.remove(entityId);
        ENTITY_TABLES.remove(entityId);
        CONTAINERS.remove(entityId);
    }

    private static final Map<Integer, ClientPowerContainer> CONTAINERS = new ConcurrentHashMap<>();

    public static @org.jetbrains.annotations.Nullable ClientPowerContainer containerFor(
            net.minecraft.world.entity.Entity entity) {
        int id = entity.getId();
        if (powersFor(id).isEmpty()) return null;
        ClientPowerContainer cached = CONTAINERS.get(id);
        if (cached != null && cached.rawOwner() == entity) return cached;
        ClientPowerContainer created = new ClientPowerContainer(entity);
        CONTAINERS.put(id, created);
        return created;
    }

    private static final Map<Integer, Map<ResourceLocation, int[]>> ENTITY_TABLES = new ConcurrentHashMap<>();

    public static void applyResourceTables(dev.overgrown.apoli.network.payload.SyncResourceTablesS2C payload) {
        if (payload.tables().isEmpty()) ENTITY_TABLES.remove(payload.entityId());
        else ENTITY_TABLES.put(payload.entityId(), Map.copyOf(payload.tables()));
    }

    public static Map<ResourceLocation, int[]> tablesFor(int entityId) {
        return ENTITY_TABLES.getOrDefault(entityId, Map.of());
    }

    private static final Map<ResourceLocation, net.minecraft.nbt.CompoundTag> LOCAL_POWER_INVENTORIES =
        new ConcurrentHashMap<>();

    public static void applyPowerInventory(dev.overgrown.apoli.network.payload.PowerInventoryS2C payload) {
        if (payload.contents() == null) LOCAL_POWER_INVENTORIES.remove(payload.powerId());
        else LOCAL_POWER_INVENTORIES.put(payload.powerId(), payload.contents());
    }

    public static net.minecraft.nbt.CompoundTag powerInventory(ResourceLocation powerId) {
        return LOCAL_POWER_INVENTORIES.get(powerId);
    }

    public static void clear() {
        LOCAL_POWER_INVENTORIES.clear();
        ENTITY_POWERS.clear();
        ENTITY_AUX.clear();
        ENTITY_SUPPRESSED.clear();
        ENTITY_TABLES.clear();
        CONTAINERS.clear();
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
