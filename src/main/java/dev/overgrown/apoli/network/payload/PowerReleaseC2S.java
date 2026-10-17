package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PowerReleaseC2S(ResourceLocation power) implements CustomPacketPayload {
    public static final Type<PowerReleaseC2S> TYPE = new Type<>(Apoli.id("power_release"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerReleaseC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        PowerReleaseC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
    }

    public static PowerReleaseC2S read(FriendlyByteBuf buf) {
        return new PowerReleaseC2S(buf.readResourceLocation());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
