package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RadialMenuSelectC2S(int nonce, int index) implements CustomPacketPayload {
    public static final Type<RadialMenuSelectC2S> TYPE = new Type<>(Apoli.id("radial_menu_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadialMenuSelectC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.nonce);
            buf.writeVarInt(payload.index);
        },
        buf -> new RadialMenuSelectC2S(buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<RadialMenuSelectC2S> type() {
        return TYPE;
    }
}
