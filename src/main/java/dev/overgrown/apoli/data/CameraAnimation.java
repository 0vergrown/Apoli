package dev.overgrown.apoli.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CameraAnimation {
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int PITCH = 3;
    public static final int YAW = 4;
    public static final int ROLL = 5;
    public static final int FOV = 6;
    public static final int CHANNELS = 7;

    public static final CameraAnimation EMPTY = new CameraAnimation(new Track[CHANNELS], 0.0F, false);

    private record Track(float[] times, float[] values, Easing[] easings) {}

    private final Track[] tracks;
    private final float duration;
    private final boolean loop;

    private CameraAnimation(Track[] tracks, float duration, boolean loop) {
        this.tracks = tracks;
        this.duration = duration;
        this.loop = loop;
    }

    public static CameraAnimation of(List<CameraKeyframe> keyframes, boolean loop) {
        if (keyframes.isEmpty()) return EMPTY;
        List<CameraKeyframe> sorted = new ArrayList<>(keyframes);
        sorted.sort(Comparator.comparingDouble(CameraKeyframe::time));
        Track[] tracks = new Track[CHANNELS];
        float duration = 0.0F;
        for (int channel = 0; channel < CHANNELS; channel++) {
            int count = 0;
            for (CameraKeyframe keyframe : sorted) {
                if (keyframe.channel(channel).isPresent()) count++;
            }
            if (count == 0) continue;
            float[] times = new float[count];
            float[] values = new float[count];
            Easing[] easings = new Easing[count];
            int at = 0;
            for (CameraKeyframe keyframe : sorted) {
                if (keyframe.channel(channel).isEmpty()) continue;
                times[at] = keyframe.time();
                values[at] = keyframe.channel(channel).get();
                easings[at] = keyframe.easing();
                at++;
            }
            tracks[channel] = new Track(times, values, easings);
            duration = Math.max(duration, times[count - 1]);
        }
        return new CameraAnimation(tracks, duration, loop);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean has(int channel) {
        return tracks[channel] != null;
    }

    public float duration() {
        return duration;
    }

    public float sample(int channel, float time) {
        Track track = tracks[channel];
        float[] times = track.times();
        float[] values = track.values();
        int last = times.length - 1;
        float t = time;
        if (loop && duration > 0.0F) {
            t = time % duration;
            if (t < 0.0F) t += duration;
        }
        if (last == 0 || t <= times[0]) return values[0];
        if (t >= times[last]) return values[last];
        int i = 0;
        while (i < last - 1 && t >= times[i + 1]) i++;
        float span = times[i + 1] - times[i];
        float u = span <= 0.0F ? 1.0F : (t - times[i]) / span;
        Easing easing = track.easings()[i];
        if (easing == Easing.STEP) return values[i];
        if (easing == Easing.CATMULLROM) {
            float p0 = values[Math.max(i - 1, 0)];
            float p1 = values[i];
            float p2 = values[i + 1];
            float p3 = values[Math.min(i + 2, last)];
            float u2 = u * u;
            float u3 = u2 * u;
            return 0.5F * ((2.0F * p1) + (-p0 + p2) * u + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * u2
                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * u3);
        }
        float eased = easing.apply(u);
        return values[i] + (values[i + 1] - values[i]) * eased;
    }
}
