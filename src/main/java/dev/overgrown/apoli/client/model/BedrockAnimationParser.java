package dev.overgrown.apoli.client.model;

import com.mojang.serialization.Dynamic;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.ModelParts;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockAnimationParser {

    private static final float[] ZERO = {0.0F, 0.0F, 0.0F};
    private static final float[] ONE = {1.0F, 1.0F, 1.0F};

    private BedrockAnimationParser() {}

    public static <T> Map<String, BedrockAnimation> parse(ResourceLocation id, Dynamic<T> json) {
        Map<Dynamic<T>, Dynamic<T>> declared = json.get("animations").result()
            .flatMap(node -> node.getMapValues().result())
            .orElse(Map.of());
        if (declared.isEmpty()) return Map.of();
        Map<String, BedrockAnimation> out = new LinkedHashMap<>(declared.size());
        for (Map.Entry<Dynamic<T>, Dynamic<T>> entry : declared.entrySet()) {
            String name = entry.getKey().asString().result().orElse(null);
            if (name == null) continue;
            try {
                out.put(name, parseAnimation(entry.getValue()));
            } catch (Exception e) {
                Apoli.LOGGER.error("[Apoli] Failed to load animation '{}' from {}: {}", name, id, e.getMessage());
            }
        }
        return out;
    }

    private static <T> BedrockAnimation parseAnimation(Dynamic<T> json) {
        BedrockAnimation.Loop loop = parseLoop(json.get("loop").result().orElse(null));
        List<BedrockAnimation.Bone> bones = new ArrayList<>();
        Map<Dynamic<T>, Dynamic<T>> declared = json.get("bones").result()
            .flatMap(node -> node.getMapValues().result())
            .orElse(Map.of());
        float longest = 0.0F;
        for (Map.Entry<Dynamic<T>, Dynamic<T>> entry : declared.entrySet()) {
            String name = entry.getKey().asString().result().orElse(null);
            if (name == null) continue;
            Dynamic<T> channels = entry.getValue();
            BedrockAnimation.Track position = parseTrack(channels.get("position").result().orElse(null), ZERO);
            BedrockAnimation.Track rotation = parseTrack(channels.get("rotation").result().orElse(null), ZERO);
            BedrockAnimation.Track scale = parseTrack(channels.get("scale").result().orElse(null), ONE);
            if (position == null && rotation == null && scale == null) continue;
            longest = Math.max(longest, longest(position, rotation, scale));
            bones.add(new BedrockAnimation.Bone(ModelParts.normalize(name), position, rotation, scale));
        }
        float length = json.get("animation_length").asNumber().result().map(Number::floatValue).orElse(longest);
        return new BedrockAnimation(length, loop, bones.toArray(new BedrockAnimation.Bone[0]));
    }

    private static float longest(@Nullable BedrockAnimation.Track... tracks) {
        float end = 0.0F;
        for (BedrockAnimation.Track track : tracks) {
            if (track != null) end = Math.max(end, track.end());
        }
        return end;
    }

    private static <T> BedrockAnimation.Loop parseLoop(@Nullable Dynamic<T> json) {
        if (json == null) return BedrockAnimation.Loop.ONCE;
        if (json.asString().result().map("hold_on_last_frame"::equals).orElse(false)) {
            return BedrockAnimation.Loop.HOLD;
        }
        return json.asBoolean(false) ? BedrockAnimation.Loop.LOOP : BedrockAnimation.Loop.ONCE;
    }

    @Nullable
    private static <T> BedrockAnimation.Track parseTrack(@Nullable Dynamic<T> json, float[] fallback) {
        if (json == null) return null;
        float[] direct = readVector(json, fallback);
        if (direct != null) {
            return new BedrockAnimation.Track(new float[]{0.0F}, direct, direct.clone());
        }
        Map<Dynamic<T>, Dynamic<T>> entries = json.getMapValues().result().orElse(null);
        if (entries == null || entries.isEmpty()) return null;
        if (entries.keySet().stream().anyMatch(key -> "pre".equals(key.asString().result().orElse(null))
            || "post".equals(key.asString().result().orElse(null)))) {
            float[] pre = keyframe(json, fallback, true);
            float[] post = keyframe(json, fallback, false);
            return new BedrockAnimation.Track(new float[]{0.0F}, pre, post);
        }

        List<Keyframe> keyframes = new ArrayList<>(entries.size());
        for (Map.Entry<Dynamic<T>, Dynamic<T>> entry : entries.entrySet()) {
            String stamp = entry.getKey().asString().result().orElse(null);
            if (stamp == null) continue;
            float time;
            try {
                time = Float.parseFloat(stamp);
            } catch (NumberFormatException e) {
                continue;
            }
            keyframes.add(new Keyframe(time, keyframe(entry.getValue(), fallback, true),
                keyframe(entry.getValue(), fallback, false)));
        }
        if (keyframes.isEmpty()) return null;
        keyframes.sort((a, b) -> Float.compare(a.time, b.time));

        int count = keyframes.size();
        float[] times = new float[count];
        float[] pre = new float[count * 3];
        float[] post = new float[count * 3];
        for (int i = 0; i < count; i++) {
            Keyframe frame = keyframes.get(i);
            times[i] = frame.time;
            System.arraycopy(frame.pre, 0, pre, i * 3, 3);
            System.arraycopy(frame.post, 0, post, i * 3, 3);
        }
        return new BedrockAnimation.Track(times, pre, post);
    }

    private static <T> float[] keyframe(Dynamic<T> json, float[] fallback, boolean pre) {
        float[] direct = readVector(json, fallback);
        if (direct != null) return direct;
        Dynamic<T> side = json.get(pre ? "pre" : "post").result().orElse(null);
        if (side == null) side = json.get(pre ? "post" : "pre").result().orElse(null);
        if (side != null) {
            float[] read = readVector(side, fallback);
            if (read != null) return read;
        }
        return fallback.clone();
    }

    @Nullable
    private static <T> float[] readVector(Dynamic<T> json, float[] fallback) {
        List<Dynamic<T>> array = json.asStreamOpt().result()
            .map(stream -> stream.toList())
            .orElse(null);
        if (array != null) {
            float[] out = fallback.clone();
            for (int i = 0; i < 3 && i < array.size(); i++) {
                out[i] = number(array.get(i));
            }
            return out;
        }
        if (json.asNumber().result().isPresent()) {
            float uniform = json.asNumber().result().get().floatValue();
            return new float[]{uniform, uniform, uniform};
        }
        return null;
    }

    private static <T> float number(Dynamic<T> json) {
        return json.asNumber().result().map(Number::floatValue).orElseGet(() -> {
            String raw = json.asString().result().orElse(null);
            if (raw == null) return 0.0F;
            try {
                return Float.parseFloat(raw.trim());
            } catch (NumberFormatException e) {
                return 0.0F;
            }
        });
    }

    private record Keyframe(float time, float[] pre, float[] post) {}
}
