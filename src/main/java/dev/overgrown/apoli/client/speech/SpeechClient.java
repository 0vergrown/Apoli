package dev.overgrown.apoli.client.speech;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.network.payload.SpeechC2S;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BiConsumer;

public final class SpeechClient {
    private static final SpeechServer SERVER = new SpeechServer();
    private static VoskTranscriber vosk;
    private static boolean opened;

    private SpeechClient() {}

    public static void onJoin() {
        String language = toBcp47(Minecraft.getInstance().options.languageCode);
        BiConsumer<String, String> onTranscript = (text, lang) ->
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null && ClientPlayNetworking.canSend(SpeechC2S.CHANNEL)) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    new SpeechC2S(text, lang == null || lang.isBlank() ? language : lang).write(buf);
                    ClientPlayNetworking.send(SpeechC2S.CHANNEL, buf);
                }
            });
        boolean started = SERVER.start(onTranscript, () -> startVoskFallback(language, onTranscript));
        if (started && !opened) {
            opened = true;
            String url = "http://127.0.0.1:" + SERVER.port() + "/?lang=" + language;
            Util.getPlatform().openUri(url);
            Minecraft.getInstance().execute(() -> announce(url));
        }
    }

    private static void announce(String url) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        net.minecraft.network.chat.Component link = net.minecraft.network.chat.Component.literal(url)
            .withStyle(style -> style
                .withColor(net.minecraft.ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, url)));
        mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "[Apoli] Speech-to-action: open this in Chrome or Edge and allow the microphone -> ").append(link), false);
    }

    public static void onLeave() {
        SERVER.stop();
        if (vosk != null) {
            vosk.stop();
            vosk = null;
        }
        opened = false;
    }

    private static void startVoskFallback(String language, BiConsumer<String, String> onTranscript) {
        if (vosk != null) {
            return;
        }
        if (!voskLibraryPresent()) {
            Apoli.LOGGER.warn("[Apoli] No Web Speech API in the browser and the Vosk library is absent — speech input is unavailable.");
            return;
        }
        Path modelDir = FabricLoader.getInstance().getGameDir().resolve("apoli").resolve("vosk-model");
        if (!Files.isDirectory(modelDir)) {
            Apoli.LOGGER.warn("[Apoli] Vosk fallback: unpack a Vosk model into {} to enable offline speech.", modelDir);
            return;
        }
        vosk = new VoskTranscriber(modelDir, language);
        vosk.start(onTranscript);
    }

    private static boolean voskLibraryPresent() {
        try {
            Class.forName("org.vosk.Model");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String toBcp47(String minecraftLanguage) {
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
