package dev.overgrown.apoli.compat.voicechat;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class VoiceState {
    private VoiceState() {}

    private static final int SPEAK_TIMEOUT_TICKS = 8;
    private static final int SILENCE_FRAME_BYTES = 16;
    private static final int LOUD_FRAME_BYTES = 160;
    private static final int LEVEL_SPAN = LOUD_FRAME_BYTES - SILENCE_FRAME_BYTES;

    private static final Map<UUID, Long> LAST_SPOKE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LOUDNESS = new ConcurrentHashMap<>();
    private static final Set<UUID> SPEAKING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> WHISPERING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> DISABLED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> DISCONNECTED = ConcurrentHashMap.newKeySet();

    private static volatile long currentTick;
    private static volatile MinecraftServer server;
    private static volatile java.util.function.Predicate<UUID> disabledCheck;
    private static BiConsumer<MinecraftServer, UUID> onSpeakStart;
    private static BiConsumer<MinecraftServer, UUID> onSpeakStop;

    public static void setServer(MinecraftServer value) {
        server = value;
    }

    public static void setCallbacks(BiConsumer<MinecraftServer, UUID> start, BiConsumer<MinecraftServer, UUID> stop) {
        onSpeakStart = start;
        onSpeakStop = stop;
    }

    public static void setDisabledCheck(java.util.function.Predicate<UUID> check) {
        disabledCheck = check;
    }

    public static void micPacketAsync(UUID uuid, int encodedBytes, boolean whispering) {
        MinecraftServer current = server;
        if (current == null) {
            return;
        }
        current.execute(() -> onMicPacket(current, uuid, encodedBytes, whispering));
    }

    public static void stateChangedAsync(UUID uuid, boolean disabled, boolean disconnected) {
        MinecraftServer current = server;
        if (current == null) {
            return;
        }
        current.execute(() -> onStateChanged(uuid, disabled, disconnected));
    }

    private static void onMicPacket(MinecraftServer current, UUID uuid, int encodedBytes, boolean whispering) {
        LAST_SPOKE.put(uuid, currentTick);
        LOUDNESS.merge(uuid, level(encodedBytes), VoiceState::envelope);
        if (whispering) {
            WHISPERING.add(uuid);
        } else {
            WHISPERING.remove(uuid);
        }
        if (SPEAKING.add(uuid) && onSpeakStart != null) {
            onSpeakStart.accept(current, uuid);
        }
    }

    private static Integer level(int encodedBytes) {
        int scaled = (encodedBytes - SILENCE_FRAME_BYTES) * 100 / LEVEL_SPAN;
        if (scaled < 0) return 0;
        return scaled > 100 ? 100 : scaled;
    }

    private static Integer envelope(Integer previous, Integer sample) {
        return sample >= previous ? sample : (previous * 3 + sample) / 4;
    }

    private static void onStateChanged(UUID uuid, boolean disabled, boolean disconnected) {
        if (disabled) {
            DISABLED.add(uuid);
        } else {
            DISABLED.remove(uuid);
        }
        if (disconnected) {
            DISCONNECTED.add(uuid);
            SPEAKING.remove(uuid);
            WHISPERING.remove(uuid);
            LOUDNESS.put(uuid, 0);
        } else {
            DISCONNECTED.remove(uuid);
        }
    }

    public static void tick(MinecraftServer current) {
        currentTick++;
        if (SPEAKING.isEmpty()) {
            return;
        }
        for (UUID uuid : SPEAKING) {
            Long last = LAST_SPOKE.get(uuid);
            if (last == null || currentTick - last > SPEAK_TIMEOUT_TICKS) {
                SPEAKING.remove(uuid);
                WHISPERING.remove(uuid);
                LOUDNESS.put(uuid, 0);
                if (onSpeakStop != null) {
                    onSpeakStop.accept(current, uuid);
                }
            }
        }
    }

    public static boolean isSpeaking(UUID uuid) {
        return SPEAKING.contains(uuid);
    }

    public static boolean isWhispering(UUID uuid) {
        return WHISPERING.contains(uuid);
    }

    public static boolean isDisabled(UUID uuid) {
        java.util.function.Predicate<UUID> check = disabledCheck;
        if (check != null) {
            return check.test(uuid);
        }
        return DISABLED.contains(uuid);
    }

    public static boolean isDisconnected(UUID uuid) {
        return DISCONNECTED.contains(uuid);
    }

    public static int loudness(UUID uuid) {
        Integer value = LOUDNESS.get(uuid);
        return value == null ? 0 : value;
    }

    public static long ticksSinceSpoke(UUID uuid) {
        Long last = LAST_SPOKE.get(uuid);
        return last == null ? Long.MAX_VALUE : currentTick - last;
    }

    public static void forget(UUID uuid) {
        LAST_SPOKE.remove(uuid);
        LOUDNESS.remove(uuid);
        SPEAKING.remove(uuid);
        WHISPERING.remove(uuid);
        DISABLED.remove(uuid);
        DISCONNECTED.remove(uuid);
    }

    public static void clear() {
        server = null;
        LAST_SPOKE.clear();
        LOUDNESS.clear();
        SPEAKING.clear();
        WHISPERING.clear();
        DISABLED.clear();
        DISCONNECTED.clear();
    }
}
