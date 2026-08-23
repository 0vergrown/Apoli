package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncResourceTablesS2C(int entityId, Map<ResourceLocation, int[]> tables) {

    public static final ResourceLocation CHANNEL = Apoli.id("sync_resource_tables");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(tables.size());
        for (Map.Entry<ResourceLocation, int[]> entry : tables.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarIntArray(entry.getValue());
        }
    }

    public static SyncResourceTablesS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        Map<ResourceLocation, int[]> tables = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation key = buf.readResourceLocation();
            tables.put(key, buf.readVarIntArray());
        }
        return new SyncResourceTablesS2C(entityId, tables);
    }
}
