package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PossessionSyncS2C(int possessorId, int hostId) implements CustomPacketPayload {
    public static final Type<PossessionSyncS2C> TYPE = new Type<>(Apoli.id("possession_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PossessionSyncS2C> STREAM_CODEC = StreamCodec.of(
        PossessionSyncS2C::write,
        PossessionSyncS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, PossessionSyncS2C payload) {
        buf.writeVarInt(payload.possessorId);
        buf.writeVarInt(payload.hostId + 1);
    }

    private static PossessionSyncS2C read(RegistryFriendlyByteBuf buf) {
        int possessorId = buf.readVarInt();
        return new PossessionSyncS2C(possessorId, buf.readVarInt() - 1);
    }

    @Override
    public Type<PossessionSyncS2C> type() {
        return TYPE;
    }
}
