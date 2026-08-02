package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.PlayerModelType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlayerModelTypeC2S(PlayerModelType modelType) implements CustomPacketPayload {
    public static final Type<PlayerModelTypeC2S> TYPE = new Type<>(Apoli.id("player_model_type"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerModelTypeC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        PlayerModelTypeC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(modelType == PlayerModelType.SLIM);
    }

    public static PlayerModelTypeC2S read(FriendlyByteBuf buf) {
        return new PlayerModelTypeC2S(buf.readBoolean() ? PlayerModelType.SLIM : PlayerModelType.WIDE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
