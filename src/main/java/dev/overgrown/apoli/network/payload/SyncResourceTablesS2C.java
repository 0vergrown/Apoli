package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record SyncResourceTablesS2C(int entityId, Map<ResourceLocation, double[]> tables) {

    public static final ResourceLocation CHANNEL = Apoli.id("sync_resource_tables");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        AuxValues.writeTables(buf, tables);
    }

    public static SyncResourceTablesS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new SyncResourceTablesS2C(entityId, AuxValues.readTables(buf));
    }
}
