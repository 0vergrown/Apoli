package dev.overgrown.apoli.compat.voicechat;

import dev.overgrown.apoli.power.builtin.ModifyHearingRangePower;
import dev.overgrown.apoli.power.builtin.ModifySpeakingRangePower;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceHearing {
    private VoiceHearing() {}

    public static final double DEFAULT_DISTANCE = 48.0;
    public static final double DEFAULT_WHISPER_DISTANCE = 24.0;

    private record Pair(UUID listener, UUID speaker) {}

    private static final Map<UUID, double[]> SPOKEN = new ConcurrentHashMap<>();
    private static final Map<UUID, double[]> BROADCAST = new ConcurrentHashMap<>();
    private static final Map<Pair, double[]> HEARD = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> ORIGINAL = new ConcurrentHashMap<>();

    private static volatile boolean active;
    private static volatile double normalBase = DEFAULT_DISTANCE;
    private static volatile double whisperBase = DEFAULT_WHISPER_DISTANCE;

    public static boolean isActive() {
        return active;
    }

    public static void setBaseDistances(double normal, double whisper) {
        if (normal > 0.0) normalBase = normal;
        if (whisper > 0.0) whisperBase = whisper;
    }

    public static void tick(MinecraftServer server) {
        boolean hearing = ModifyHearingRangePower.inUse();
        boolean speaking = ModifySpeakingRangePower.inUse();
        if (!hearing && !speaking) {
            if (active) reset();
            return;
        }
        double normal = normalBase;
        double whisper = whisperBase;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int count = players.size();
        boolean any = false;

        SPOKEN.clear();
        for (int i = 0; speaking && i < count; i++) {
            ServerPlayer player = players.get(i);
            double[] spoken = ModifySpeakingRangePower.ranges(player, normal, whisper);
            if (spoken != null) {
                SPOKEN.put(player.getUUID(), spoken);
                any = true;
            } else if (!any && ModifySpeakingRangePower.targetsListeners(player)) {
                any = true;
            }
        }

        HEARD.clear();
        BROADCAST.clear();
        for (int i = 0; i < count; i++) {
            ServerPlayer speaker = players.get(i);
            UUID speakerId = speaker.getUUID();
            double[] spoken = SPOKEN.get(speakerId);
            double spokenNormal = spoken == null ? normal : spoken[0];
            double spokenWhisper = spoken == null ? whisper : spoken[1];
            double widestNormal = spokenNormal;
            double widestWhisper = spokenWhisper;
            boolean talking = VoiceState.isSpeaking(speakerId);
            boolean targeted = talking && speaking && ModifySpeakingRangePower.targetsListeners(speaker);
            if (talking && (hearing || targeted)) {
                for (int j = 0; j < count; j++) {
                    if (i == j) continue;
                    ServerPlayer listener = players.get(j);
                    double[] pair = pairRanges(speaker, listener, spokenNormal, spokenWhisper, hearing, targeted);
                    if (pair == null) continue;
                    HEARD.put(new Pair(listener.getUUID(), speakerId), pair);
                    if (pair[0] > widestNormal) widestNormal = pair[0];
                    if (pair[1] > widestWhisper) widestWhisper = pair[1];
                }
            }
            if (spoken != null || widestNormal != normal || widestWhisper != whisper) {
                BROADCAST.put(speakerId, new double[]{widestNormal, widestWhisper});
            }
        }

        if (hearing && !any) {
            for (int i = 0; i < count; i++) {
                if (ModifyHearingRangePower.hearsDifferently(players.get(i))) {
                    any = true;
                    break;
                }
            }
        }

        active = any;
        if (!any) ORIGINAL.clear();
    }

    private static double @Nullable [] pairRanges(ServerPlayer speaker, ServerPlayer listener, double spokenNormal,
                                                  double spokenWhisper, boolean hearing, boolean targeted) {
        double[] pair = targeted
            ? ModifySpeakingRangePower.rangesToward(speaker, listener, spokenNormal, spokenWhisper)
            : null;
        double normal = pair == null ? spokenNormal : pair[0];
        double whisper = pair == null ? spokenWhisper : pair[1];
        if (hearing && ModifyHearingRangePower.hearsDifferently(listener)) {
            double[] heard = ModifyHearingRangePower.voiceRanges(listener, speaker, normal, whisper);
            if (heard != null) {
                if (normal <= 0.0) heard[0] = 0.0;
                if (whisper <= 0.0) heard[1] = 0.0;
                pair = heard;
            }
        }
        return pair;
    }

    public static double reach(ServerPlayer listener, ServerPlayer speaker, boolean whispering) {
        double normal = normalBase;
        double whisper = whisperBase;
        boolean speaking = ModifySpeakingRangePower.inUse();
        boolean hearing = ModifyHearingRangePower.inUse();
        if (speaking) {
            double[] spoken = ModifySpeakingRangePower.ranges(speaker, normal, whisper);
            if (spoken != null) {
                normal = spoken[0];
                whisper = spoken[1];
            }
        }
        if (speaking || hearing) {
            double[] pair = pairRanges(speaker, listener, normal, whisper, hearing,
                speaking && ModifySpeakingRangePower.targetsListeners(speaker));
            if (pair != null) {
                normal = pair[0];
                whisper = pair[1];
            }
        }
        return whispering ? whisper : normal;
    }

    public static void forget(UUID uuid) {
        SPOKEN.remove(uuid);
        BROADCAST.remove(uuid);
        ORIGINAL.remove(uuid);
        HEARD.keySet().removeIf(pair -> pair.listener().equals(uuid) || pair.speaker().equals(uuid));
    }

    public static void reset() {
        active = false;
        SPOKEN.clear();
        BROADCAST.clear();
        HEARD.clear();
        ORIGINAL.clear();
    }

    public static float broadcastDistance(UUID speaker, float base, boolean whispering) {
        if (whispering) {
            whisperBase = base;
        } else {
            normalBase = base;
        }
        double[] spoken = SPOKEN.get(speaker);
        float voice = spoken == null ? base : (float) (whispering ? spoken[1] : spoken[0]);
        ORIGINAL.put(speaker, voice);
        double[] widest = BROADCAST.get(speaker);
        float reach = widest == null ? voice : (float) (whispering ? widest[1] : widest[0]);
        if (voice > reach) reach = voice;
        return reach > 0.0F ? reach : 0.0F;
    }

    public static float originalDistance(UUID speaker, float sent) {
        Float recorded = ORIGINAL.get(speaker);
        return recorded == null ? sent : recorded;
    }

    public static double rangeFor(UUID listener, UUID speaker, boolean whispering, double fallback) {
        double[] heard = HEARD.get(new Pair(listener, speaker));
        if (heard == null) return fallback;
        return whispering ? heard[1] : heard[0];
    }
}
