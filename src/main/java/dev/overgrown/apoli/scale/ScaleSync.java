package dev.overgrown.apoli.scale;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

public final class ScaleSync {
    private ScaleSync() {}

    public static byte[] encode(Entity entity) {
        ScaleState state = Scales.stateOf(entity);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            if (state == null) {
                buf.writeVarInt(0);
            } else {
                state.write(buf);
            }
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    public static void decode(Entity entity, byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            ScaleState state = Scales.stateOrCreate(entity);
            state.read(buf);
            Scales.refreshDimensions(entity, state);
        } finally {
            buf.release();
        }
    }
}
