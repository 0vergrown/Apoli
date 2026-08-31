package dev.overgrown.apoli.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SavedLocations extends SavedData {

    public static final String FILE_ID = "apoli_locations";

    private static final int MAX_ENTITIES = 4096;
    private static final int MAX_PER_ENTITY = 32;

    public record Location(double x, double y, double z, ResourceKey<Level> dimension, float yaw, float pitch) {}

    private static final Codec<Location> LOCATION = RecordCodecBuilder.create(i -> i.group(
        Codec.DOUBLE.fieldOf("x").forGetter(Location::x),
        Codec.DOUBLE.fieldOf("y").forGetter(Location::y),
        Codec.DOUBLE.fieldOf("z").forGetter(Location::z),
        ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Location::dimension),
        Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(Location::yaw),
        Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(Location::pitch)
    ).apply(i, Location::new));

    private static final Codec<Map<UUID, Map<String, Location>>> CODEC =
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Codec.STRING, LOCATION));

    private final Map<UUID, Map<String, Location>> byEntity =
        new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Map<String, Location>> eldest) {
                return size() > MAX_ENTITIES;
            }
        };

    public static SavedLocations get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(SavedLocations::new, SavedLocations::load, DataFixTypes.LEVEL), FILE_ID);
    }

    public static @Nullable SavedLocations of(Entity entity) {
        MinecraftServer server = entity.getServer();
        return server == null ? null : get(server);
    }

    public static SavedLocations load(CompoundTag tag, HolderLookup.Provider registries) {
        SavedLocations state = new SavedLocations();
        Tag stored = tag.get("locations");
        if (stored == null) return state;
        CODEC.parse(NbtOps.INSTANCE, stored)
            .resultOrPartial(error -> Apoli.LOGGER.error("[Apoli] Could not read saved locations: {}", error))
            .ifPresent(map -> map.forEach((id, locations) -> {
                if (locations.isEmpty()) return;
                state.byEntity.put(id, new LinkedHashMap<>(locations));
            }));
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, byEntity)
            .resultOrPartial(error -> Apoli.LOGGER.error("[Apoli] Could not write saved locations: {}", error))
            .ifPresent(encoded -> tag.put("locations", encoded));
        return tag;
    }

    public void put(UUID entity, String id, Location location, boolean overwrite) {
        Map<String, Location> locations = byEntity.computeIfAbsent(entity, key -> new LinkedHashMap<>(4));
        if (!overwrite && locations.containsKey(id)) return;
        locations.put(id, location);
        while (locations.size() > MAX_PER_ENTITY) {
            locations.remove(locations.keySet().iterator().next());
        }
        setDirty();
    }

    public @Nullable Location get(UUID entity, String id) {
        Map<String, Location> locations = byEntity.get(entity);
        return locations == null ? null : locations.get(id);
    }

    public boolean has(UUID entity, String id) {
        return get(entity, id) != null;
    }

    public boolean remove(UUID entity, String id) {
        Map<String, Location> locations = byEntity.get(entity);
        if (locations == null || locations.remove(id) == null) return false;
        if (locations.isEmpty()) byEntity.remove(entity);
        setDirty();
        return true;
    }

    public boolean removeAll(UUID entity) {
        if (byEntity.remove(entity) == null) return false;
        setDirty();
        return true;
    }

    public Set<String> ids(UUID entity) {
        Map<String, Location> locations = byEntity.get(entity);
        return locations == null ? Set.of() : Set.copyOf(locations.keySet());
    }
}
