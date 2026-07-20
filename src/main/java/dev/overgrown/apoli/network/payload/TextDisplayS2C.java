package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.TextBar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TextDisplayS2C(TextBar bar, Component text, int fadeIn, int stay, int fadeOut) implements CustomPacketPayload {
    public static final Type<TextDisplayS2C> TYPE = new Type<>(Apoli.id("text_display"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TextDisplayS2C> STREAM_CODEC = StreamCodec.of(
        TextDisplayS2C::write,
        TextDisplayS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, TextDisplayS2C payload) {
        buf.writeByte(payload.bar.ordinal());
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, payload.text);
        buf.writeVarInt(payload.fadeIn);
        buf.writeVarInt(payload.stay);
        buf.writeVarInt(payload.fadeOut);
    }

    private static TextDisplayS2C read(RegistryFriendlyByteBuf buf) {
        TextBar bar = TextBar.VALUES[Math.floorMod(buf.readByte(), TextBar.VALUES.length)];
        Component text = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        int fadeIn = buf.readVarInt();
        int stay = buf.readVarInt();
        int fadeOut = buf.readVarInt();
        return new TextDisplayS2C(bar, text, fadeIn, stay, fadeOut);
    }

    @Override
    public Type<TextDisplayS2C> type() {
        return TYPE;
    }
}
