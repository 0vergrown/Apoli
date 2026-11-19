package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record JumpC2S() implements CustomPacketPayload {
    public static final JumpC2S INSTANCE = new JumpC2S();
    public static final Type<JumpC2S> TYPE = new Type<>(Apoli.id("jump"));
    public static final StreamCodec<ByteBuf, JumpC2S> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<JumpC2S> type() {
        return TYPE;
    }
}
