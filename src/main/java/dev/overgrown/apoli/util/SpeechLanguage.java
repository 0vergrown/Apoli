package dev.overgrown.apoli.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class SpeechLanguage {
    private SpeechLanguage() {}

    public static String of(ServerPlayer player) {
        return toBcp47(player.clientInformation().language());
    }

    public static String toBcp47(String minecraftLanguage) {
        if (minecraftLanguage == null || minecraftLanguage.isBlank()) {
            return "en-US";
        }
        String[] parts = minecraftLanguage.split("_");
        if (parts.length >= 2) {
            return parts[0] + "-" + parts[1].toUpperCase(Locale.ROOT);
        }
        return parts[0];
    }
}
