package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record SyncEntityPowersS2C(
    int entityId,
    Map<ResourceLocation, Set<ResourceLocation>> powersBySource,
    Map<ResourceLocation, Integer> auxInt,
    Set<ResourceLocation> suppressed
) {
    public static final ResourceLocation CHANNEL = Apoli.id("sync_entity_powers");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(powersBySource.size());
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> e : powersBySource.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeCollection(e.getValue(), FriendlyByteBuf::writeResourceLocation);
        }
        buf.writeVarInt(auxInt.size());
        for (Map.Entry<ResourceLocation, Integer> e : auxInt.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue());
        }
        buf.writeCollection(suppressed, FriendlyByteBuf::writeResourceLocation);
    }

    public static SyncEntityPowersS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int n = buf.readVarInt();
        Map<ResourceLocation, Set<ResourceLocation>> map = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation key = buf.readResourceLocation();
            Set<ResourceLocation> srcs = new HashSet<>(buf.readList(FriendlyByteBuf::readResourceLocation));
            map.put(key, srcs);
        }
        int m = buf.readVarInt();
        Map<ResourceLocation, Integer> aux = new HashMap<>(m);
        for (int i = 0; i < m; i++) {
            ResourceLocation key = buf.readResourceLocation();
            int value = buf.readVarInt();
            aux.put(key, value);
        }
        Set<ResourceLocation> suppressed = new HashSet<>(buf.readList(FriendlyByteBuf::readResourceLocation));
        return new SyncEntityPowersS2C(entityId, map, aux, suppressed);
    }
}
