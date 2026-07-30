package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ForceKeyS2C(String key, int duration, boolean release) implements CustomPacketPayload {
    public static final Type<ForceKeyS2C> TYPE = new Type<>(Apoli.id("force_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceKeyS2C> STREAM_CODEC = StreamCodec.of(
        ForceKeyS2C::write,
        ForceKeyS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, ForceKeyS2C payload) {
        buf.writeUtf(payload.key);
        buf.writeVarInt(payload.duration);
        buf.writeBoolean(payload.release);
    }

    private static ForceKeyS2C read(RegistryFriendlyByteBuf buf) {
        String key = buf.readUtf();
        int duration = buf.readVarInt();
        boolean release = buf.readBoolean();
        return new ForceKeyS2C(key, duration, release);
    }

    @Override
    public Type<ForceKeyS2C> type() {
        return TYPE;
    }
}
