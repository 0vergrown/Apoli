package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;

import java.util.List;
import java.util.Map;

public record SequenceStep(Key key, int taps, int hold, int maxGap) {

    public static final MapCodec<SequenceStep> MAP_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<SequenceStep>mapCodec(i -> i.group(
            Key.CODEC.fieldOf("key").forGetter(SequenceStep::key),
            Codec.intRange(1, 16).optionalFieldOf("taps", 1).forGetter(SequenceStep::taps),
            Codec.intRange(0, 1200).optionalFieldOf("hold", 0).forGetter(SequenceStep::hold),
            Codec.intRange(0, 1200).optionalFieldOf("max_gap", 0).forGetter(SequenceStep::maxGap)
        ).apply(i, SequenceStep::new)),
        Map.of("presses", "taps", "hold_ticks", "hold", "window", "max_gap"));

    public static final Codec<SequenceStep> CODEC = Codec.either(Codec.STRING, MAP_CODEC.codec()).xmap(
        either -> either.map(SequenceStep::press, step -> step),
        step -> step.isPlainPress() ? Either.left(step.key.key()) : Either.right(step)
    );

    public static SequenceStep press(String key) {
        return new SequenceStep(new Key(key, false), 1, 0, 0);
    }

    public boolean isPlainPress() {
        return taps == 1 && hold == 0 && maxGap == 0 && !key.continuous();
    }

    public static void expand(List<SequenceStep> steps, List<String> keysOut, int[] holdOut, int[] gapOut) {
        int at = 0;
        for (int s = 0; s < steps.size(); s++) {
            SequenceStep step = steps.get(s);
            for (int tap = 0; tap < step.taps; tap++) {
                keysOut.add(step.key.key());
                holdOut[at] = tap == step.taps - 1 ? step.hold : 0;
                gapOut[at] = at == 0 ? 0 : step.maxGap;
                at++;
            }
        }
    }

    public static int atomCount(List<SequenceStep> steps) {
        int total = 0;
        for (int i = 0; i < steps.size(); i++) total += steps.get(i).taps;
        return total;
    }
}
