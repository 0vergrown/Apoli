package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncAuxIntsS2C(int entityId, Map<ResourceLocation, Integer> auxInt) implements CustomPacketPayload {

    public static final Type<SyncAuxIntsS2C> TYPE = new Type<>(Apoli.id("sync_aux_ints"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAuxIntsS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncAuxIntsS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(auxInt.size());
        for (Map.Entry<ResourceLocation, Integer> entry : auxInt.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public static SyncAuxIntsS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        Map<ResourceLocation, Integer> values = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            values.put(buf.readResourceLocation(), buf.readVarInt());
        }
        return new SyncAuxIntsS2C(entityId, values);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
