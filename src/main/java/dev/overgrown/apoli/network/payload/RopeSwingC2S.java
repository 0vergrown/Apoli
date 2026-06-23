package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record RopeSwingC2S(Vec3 inputDir) implements CustomPacketPayload {

    public static final Type<RopeSwingC2S> TYPE = new Type<>(Apoli.id("rope_swing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RopeSwingC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        RopeSwingC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(inputDir.x);
        buf.writeDouble(inputDir.y);
        buf.writeDouble(inputDir.z);
    }

    public static RopeSwingC2S read(FriendlyByteBuf buf) {
        return new RopeSwingC2S(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
