package dev.overgrown.apoli.compat.voicechat;

import dev.overgrown.apoli.power.builtin.ModifyHearingRangePower;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceHearing {
    private VoiceHearing() {}

    public static final double DEFAULT_DISTANCE = 48.0;
    public static final double DEFAULT_WHISPER_DISTANCE = 24.0;

    private static final Map<UUID, double[]> RANGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> WIDENED = new ConcurrentHashMap<>();

    private static volatile boolean active;
    private static volatile double widestNormal;
    private static volatile double widestWhisper;
    private static volatile double normalBase = DEFAULT_DISTANCE;
    private static volatile double whisperBase = DEFAULT_WHISPER_DISTANCE;

    public static boolean isActive() {
        return active;
    }

    public static void tick(MinecraftServer server) {
        if (!ModifyHearingRangePower.inUse()) {
            if (active || !RANGES.isEmpty()) reset();
            return;
        }
        double normal = normalBase;
        double whisper = whisperBase;
        double maxNormal = 0.0;
        double maxWhisper = 0.0;
        boolean any = false;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int i = 0, n = players.size(); i < n; i++) {
            ServerPlayer player = players.get(i);
            UUID uuid = player.getUUID();
            double[] ranges = ModifyHearingRangePower.voiceRanges(player, normal, whisper);
            if (ranges == null) {
                RANGES.remove(uuid);
                continue;
            }
            RANGES.put(uuid, ranges);
            any = true;
            if (ranges[0] > maxNormal) maxNormal = ranges[0];
            if (ranges[1] > maxWhisper) maxWhisper = ranges[1];
        }
        widestNormal = maxNormal;
        widestWhisper = maxWhisper;
        active = any;
        if (!any && !WIDENED.isEmpty()) WIDENED.clear();
    }

    public static void forget(UUID uuid) {
        RANGES.remove(uuid);
        WIDENED.remove(uuid);
    }

    public static void reset() {
        active = false;
        widestNormal = 0.0;
        widestWhisper = 0.0;
        RANGES.clear();
        WIDENED.clear();
    }

    public static float broadcastDistance(UUID speaker, float base, boolean whispering) {
        if (whispering) {
            whisperBase = base;
        } else {
            normalBase = base;
        }
        double widest = whispering ? widestWhisper : widestNormal;
        if (widest <= base) {
            WIDENED.remove(speaker);
            return base;
        }
        WIDENED.put(speaker, base);
        return (float) widest;
    }

    public static float originalDistance(UUID speaker, float sent) {
        Float recorded = WIDENED.get(speaker);
        return recorded == null ? sent : recorded;
    }

    public static double rangeFor(UUID listener, boolean whispering, double fallback) {
        double[] ranges = RANGES.get(listener);
        if (ranges == null) return fallback;
        return whispering ? ranges[1] : ranges[0];
    }
}
