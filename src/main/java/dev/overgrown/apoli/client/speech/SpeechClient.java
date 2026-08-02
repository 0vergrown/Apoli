package dev.overgrown.apoli.client.speech;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.client.ApoliClientConfig;
import dev.overgrown.apoli.client.ApoliKeyMappings;
import dev.overgrown.apoli.compat.voicechat.ActionOnSpeechPower;
import dev.overgrown.apoli.compat.voicechat.SpeechAudioBus;
import dev.overgrown.apoli.network.payload.SpeechTriggerC2S;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SpeechClient {
    private static final int RETRY_TICKS = 40;

    private static final java.util.Set<net.minecraft.resources.ResourceLocation> FIRED = new java.util.HashSet<>();

    private static final VoskTranscriber.Listener LISTENER = new VoskTranscriber.Listener() {
        @Override
        public void text(String text, boolean complete) {
            if (!complete && !ApoliClientConfig.get().speechInstant()) return;
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> matchAndSend(mc, text, complete));
        }

        @Override
        public void utteranceEnded() {
            Minecraft.getInstance().execute(FIRED::clear);
        }
    };

    private static VoskTranscriber transcriber;
    private static KeyMapping pushToTalk;
    private static String lastReport;
    private static int retry;

    private SpeechClient() {}

    public static void setPushToTalkKey(KeyMapping key) {
        pushToTalk = key;
    }

    public static void onJoin() {
        lastReport = null;
        retry = 0;
    }

    public static void onLeave() {
        SpeechAudioBus.setSink(null);
        if (transcriber != null) {
            transcriber.stop();
            transcriber = null;
        }
        lastReport = null;
        retry = 0;
        FIRED.clear();
    }

    public static void clientTick(Minecraft mc) {
        if (!ApoliClientConfig.get().speechToAction()) return;
        if (mc.player == null) return;

        if (transcriber == null) {
            if (retry-- > 0) return;
            retry = RETRY_TICKS;
            start(mc);
            return;
        }
        if (transcriber.hasFailed()) {
            String reason = transcriber.failure();
            transcriber.stop();
            transcriber = null;
            retry = RETRY_TICKS * 10;
            say(mc, "Speech-to-action stopped — " + reason, ChatFormatting.RED);
            return;
        }
        if (transcriber.consumeSilentCapture()) {
            say(mc, "No audio was captured. Another program (Simple Voice Chat?) may have the microphone. "
                + "Pick a different device with \"speechInputDevice\" in apoli-client.json — the available ones "
                + "are listed in the log.", ChatFormatting.YELLOW);
        }
        if (useVoiceChat()) return;
        if (mc.screen != null) {
            transcriber.setListening(false);
            return;
        }
        if (!ApoliClientConfig.get().speechPushToTalk()) {
            transcriber.setListening(true);
            return;
        }
        transcriber.setListening(pushToTalk != null && !pushToTalk.isUnbound()
            && ApoliKeyMappings.isDown(pushToTalk));
    }

    private static void start(Minecraft mc) {
        if (!voskPresent()) {
            report(mc, "no-vosk", "Speech-to-action could not start its speech engine — " + VoskRuntime.failure(),
                ChatFormatting.YELLOW);
            return;
        }
        Path modelDir = modelDir();
        if (!Files.isDirectory(modelDir)) {
            report(mc, "no-model", "Speech-to-action is on, but no Vosk model is installed. Unpack one from "
                + "https://alphacephei.com/vosk/models into " + modelDir, ChatFormatting.YELLOW);
            return;
        }
        if (useVoiceChat()) {
            transcriber = new VoskTranscriber(modelDir, "", VoskTranscriber.Source.VOICE_CHAT, LISTENER);
            transcriber.start();
            SpeechAudioBus.setSink(pcm -> {
                VoskTranscriber current = transcriber;
                if (current != null) current.offer(pcm);
            });
            report(mc, "ready-vc", "Speech-to-action ready — it listens to your Simple Voice Chat microphone, "
                + "so just talk on voice chat.", ChatFormatting.GRAY);
            return;
        }

        boolean pushToTalkMode = ApoliClientConfig.get().speechPushToTalk();
        if (pushToTalkMode && (pushToTalk == null || pushToTalk.isUnbound())) {
            report(mc, "no-key", "Speech-to-action is on, but no push-to-talk key is bound. Bind \"Speech to Action\" "
                + "under Options > Controls > Apoli, or set \"speechPushToTalk\": false in apoli-client.json.",
                ChatFormatting.YELLOW);
            return;
        }
        transcriber = new VoskTranscriber(modelDir, ApoliClientConfig.get().speechInputDevice(),
            VoskTranscriber.Source.MICROPHONE, LISTENER);
        transcriber.start();
        report(mc, "ready", pushToTalkMode
            ? "Speech-to-action ready — hold " + pushToTalk.getTranslatedKeyMessage().getString() + " to speak."
            : "Speech-to-action ready — listening continuously.", ChatFormatting.GRAY);
    }

    private static boolean useVoiceChat() {
        String mode = ApoliClientConfig.get().speechSource();
        if (mode.equalsIgnoreCase("microphone")) return false;
        if (mode.equalsIgnoreCase("voicechat")) return true;
        return dev.overgrown.apoli.compat.ModCompat.VOICECHAT;
    }

    private static boolean voskPresent() {
        return VoskRuntime.available();
    }

    private static Path modelDir() {
        return FMLPaths.GAMEDIR.get().resolve("apoli").resolve("vosk-model");
    }

    private static void report(Minecraft mc, String id, String message, ChatFormatting colour) {
        if (id.equals(lastReport)) return;
        lastReport = id;
        say(mc, message, colour);
    }

    private static void say(Minecraft mc, String message, ChatFormatting colour) {
        if (mc.player == null) return;
        mc.player.displayClientMessage(Component.literal("[Apoli] " + message).withStyle(colour), false);
    }

    private static void matchAndSend(Minecraft mc, String text, boolean complete) {
        if (mc.player == null) return;
        if (complete && ApoliClientConfig.get().speechEcho()) {
            say(mc, "heard: \"" + text + "\"", ChatFormatting.DARK_GRAY);
        }
        if (mc.getConnection() == null || !mc.getConnection().hasChannel(SpeechTriggerC2S.TYPE)) {
            Apoli.LOGGER.warn("[Apoli] Speech matched but the server has no apoli:speech_trigger channel "
                + "— it is running an older Apoli.");
            return;
        }

        String language = dev.overgrown.apoli.util.SpeechLanguage.toBcp47(mc.options.languageCode);
        int[] matched = new int[1];
        PowerLookup.forEachEntry(mc.player, ApoliIds.ACTION_ON_SPEECH, ActionOnSpeechPower.Config.class,
            (powerId, cfg) -> {
                if (ActionOnSpeechPower.matchedTrigger(cfg, text, language) == null) return;
                matched[0]++;
                if (!FIRED.add(powerId)) return;
                PacketDistributor.sendToServer(new SpeechTriggerC2S(powerId));
            });
        if (complete && matched[0] == 0 && ApoliClientConfig.get().speechEcho()) {
            say(mc, "no action_on_speech power matched that.", ChatFormatting.DARK_GRAY);
        }
    }
}
