package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PowerActivatedS2C(ResourceLocation power, int cooldown) implements CustomPacketPayload {
    public static final Type<PowerActivatedS2C> TYPE = new Type<>(Apoli.id("power_activated"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerActivatedS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        PowerActivatedS2C::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(power);
        buf.writeVarInt(cooldown);
    }

    public static PowerActivatedS2C read(FriendlyByteBuf buf) {
        return new PowerActivatedS2C(buf.readResourceLocation(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
