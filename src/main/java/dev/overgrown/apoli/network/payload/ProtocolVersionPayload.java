package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProtocolVersionPayload(int version) implements CustomPacketPayload {
    public static final Type<ProtocolVersionPayload> TYPE = new Type<>(Apoli.id("protocol_version"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProtocolVersionPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeVarInt(payload.version()),
        buf -> new ProtocolVersionPayload(buf.readVarInt()));

    @Override
    public Type<ProtocolVersionPayload> type() {
        return TYPE;
    }
}
