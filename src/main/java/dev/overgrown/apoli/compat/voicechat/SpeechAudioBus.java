package dev.overgrown.apoli.compat.voicechat;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class SpeechAudioBus {
    public static final int SAMPLE_RATE = 48000;

    @Nullable
    private static volatile Consumer<short[]> sink;

    private SpeechAudioBus() {}

    public static void setSink(@Nullable Consumer<short[]> consumer) {
        sink = consumer;
    }

    public static boolean hasSink() {
        return sink != null;
    }

    public static void push(short[] pcm) {
        Consumer<short[]> target = sink;
        if (target != null && pcm != null && pcm.length > 0) target.accept(pcm);
    }
}
