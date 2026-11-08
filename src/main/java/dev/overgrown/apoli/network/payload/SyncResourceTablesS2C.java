package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record SyncResourceTablesS2C(int entityId, Map<ResourceLocation, double[]> tables) implements CustomPacketPayload {

    public static final Type<SyncResourceTablesS2C> TYPE = new Type<>(Apoli.id("sync_resource_tables"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncResourceTablesS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncResourceTablesS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        AuxValues.writeTables(buf, tables);
    }

    public static SyncResourceTablesS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new SyncResourceTablesS2C(entityId, AuxValues.readTables(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
