package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SpeechC2S(String text, String language) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 512;
    public static final Type<SpeechC2S> TYPE = new Type<>(Apoli.id("speech"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeechC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.write(buf),
        SpeechC2S::read);

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text, MAX_LENGTH);
        buf.writeUtf(language, 32);
    }

    public static SpeechC2S read(FriendlyByteBuf buf) {
        String text = buf.readUtf(MAX_LENGTH);
        String language = buf.readUtf(32);
        return new SpeechC2S(text, language);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
