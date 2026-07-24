package dev.overgrown.apoli.network.payload;

import dev.overgrown.apoli.Apoli;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SpeechC2S(String text, String language) {
    public static final ResourceLocation CHANNEL = Apoli.id("speech");

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(text);
        buf.writeUtf(language);
    }

    public static SpeechC2S read(FriendlyByteBuf buf) {
        String text = buf.readUtf();
        String language = buf.readUtf();
        return new SpeechC2S(text, language);
    }
}
