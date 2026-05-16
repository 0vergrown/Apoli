package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PowerActivationC2S(ResourceLocation power) implements CustomPacketPayload {
    public static final Type<PowerActivationC2S> TYPE = new Type<>(Apoli.id("power_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerActivationC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        PowerActivationC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
    }

    public static PowerActivationC2S read(FriendlyByteBuf buf) {
        return new PowerActivationC2S(buf.readResourceLocation());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
