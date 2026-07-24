package dev.overgrown.apoli.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class SpeechLanguage {
    private SpeechLanguage() {}

    public static String of(ServerPlayer player) {
        String language = player.clientInformation().language();
        if (language == null || language.isBlank()) {
            return "en-US";
        }
        String[] parts = language.split("_");
        if (parts.length >= 2) {
            return parts[0] + "-" + parts[1].toUpperCase(Locale.ROOT);
        }
        return parts[0];
    }
}
