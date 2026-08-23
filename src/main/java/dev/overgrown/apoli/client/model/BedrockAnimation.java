package dev.overgrown.apoli.client.model;

import org.jetbrains.annotations.Nullable;

public final class BedrockAnimation {

    public enum Loop { ONCE, HOLD, LOOP }

    private final float length;
    private final Loop loop;
    private final Bone[] bones;

    public BedrockAnimation(float length, Loop loop, Bone[] bones) {
        this.length = length;
        this.loop = loop;
        this.bones = bones;
    }

    public float length() {
        return this.length;
    }

    public Loop loop() {
        return this.loop;
    }

    public Bone[] bones() {
        return this.bones;
    }

    public float timeFor(float elapsed, @Nullable Boolean loopOverride) {
        Loop mode = loopOverride == null ? this.loop : (loopOverride ? Loop.LOOP : Loop.ONCE);
        if (this.length <= 0.0F) return 0.0F;
        return switch (mode) {
            case LOOP -> elapsed % this.length;
            case HOLD -> Math.min(elapsed, this.length);
            case ONCE -> elapsed > this.length ? -1.0F : elapsed;
        };
    }

    public static final class Bone {
        public final String name;
        @Nullable public final Track position;
        @Nullable public final Track rotation;
        @Nullable public final Track scale;

        public Bone(String name, @Nullable Track position, @Nullable Track rotation, @Nullable Track scale) {
            this.name = name;
            this.position = position;
            this.rotation = rotation;
            this.scale = scale;
        }
    }

    public static final class Track {
        private final float[] times;
        private final float[] pre;
        private final float[] post;

        public Track(float[] times, float[] pre, float[] post) {
            this.times = times;
            this.pre = pre;
            this.post = post;
        }

        public float end() {
            return this.times.length == 0 ? 0.0F : this.times[this.times.length - 1];
        }

        public void sample(float time, float[] out) {
            int count = this.times.length;
            if (count == 0) return;
            if (time <= this.times[0]) {
                copy(this.pre, 0, out);
                return;
            }
            int last = count - 1;
            if (time >= this.times[last]) {
                copy(this.post, last, out);
                return;
            }
            int index = 0;
            while (index < last && this.times[index + 1] <= time) index++;
            float span = this.times[index + 1] - this.times[index];
            float delta = span <= 0.0F ? 0.0F : (time - this.times[index]) / span;
            int from = index * 3;
            int to = (index + 1) * 3;
            out[0] = this.post[from] + (this.pre[to] - this.post[from]) * delta;
            out[1] = this.post[from + 1] + (this.pre[to + 1] - this.post[from + 1]) * delta;
            out[2] = this.post[from + 2] + (this.pre[to + 2] - this.post[from + 2]) * delta;
        }

        private static void copy(float[] source, int keyframe, float[] out) {
            int base = keyframe * 3;
            out[0] = source[base];
            out[1] = source[base + 1];
            out[2] = source[base + 2];
        }
    }
}
