package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ProtocolVersionPayload(int version) {
    public static final ResourceLocation CHANNEL = Apoli.id("protocol_version");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(version);
    }

    public static ProtocolVersionPayload read(FriendlyByteBuf buf) {
        return new ProtocolVersionPayload(buf.readVarInt());
    }
}
