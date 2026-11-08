package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncAuxValuesS2C(int entityId, Object2DoubleMap<ResourceLocation> values) implements CustomPacketPayload {

    public static final Type<SyncAuxValuesS2C> TYPE = new Type<>(Apoli.id("sync_aux_values"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAuxValuesS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SyncAuxValuesS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        AuxValues.write(buf, values);
    }

    public static SyncAuxValuesS2C read(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        return new SyncAuxValuesS2C(entityId, AuxValues.read(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
