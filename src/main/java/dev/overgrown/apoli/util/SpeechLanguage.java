package dev.overgrown.apoli.util;

import net.minecraft.server.level.ServerPlayer;

public final class SpeechLanguage {
    private SpeechLanguage() {}

    public static String of(ServerPlayer player) {
        return "en-US";
    }
}
