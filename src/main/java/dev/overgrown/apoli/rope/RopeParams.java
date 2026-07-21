package dev.overgrown.apoli.rope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import static dev.overgrown.apoli.rope.RopeConstants.*;

public record RopeParams(
    float minLength,
    float maxLength,
    float stiffness,
    float radialDamping,
    float springScaling,
    float swingBoost,
    float maxSwingSpeed,
    float controlAccel,
    float reelStep,
    float slackPullRate,
    boolean constrainFrom,
    boolean constrainTo,
    boolean controllable,
    Mode mode,
    float breakBeyond
) {

    public enum Mode implements StringRepresentable {

        LEASH("leash"),

        SPRING("spring"),

        RIGID("rigid");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final RopeParams DEFAULT = new RopeParams(
        MIN_ROPE_LENGTH, 30f, LEASH_STIFFNESS, RADIAL_DAMPING, SPRING_SCALING,
        SWING_BOOST, MAX_SWING_SPEED, SWING_CONTROL_ACCEL, ROPE_LENGTH_CHANGE_STEP, SLACK_PULL_RATE_MULT,
        true, true, true, Mode.LEASH, 0f);

    public static final MapCodec<RopeParams> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.FLOAT.optionalFieldOf("min_length", DEFAULT.minLength).forGetter(RopeParams::minLength),
        Codec.FLOAT.optionalFieldOf("max_length", DEFAULT.maxLength).forGetter(RopeParams::maxLength),
        Codec.FLOAT.optionalFieldOf("stiffness", DEFAULT.stiffness).forGetter(RopeParams::stiffness),
        Codec.FLOAT.optionalFieldOf("radial_damping", DEFAULT.radialDamping).forGetter(RopeParams::radialDamping),
        Codec.FLOAT.optionalFieldOf("spring_scaling", DEFAULT.springScaling).forGetter(RopeParams::springScaling),
        Codec.FLOAT.optionalFieldOf("swing_boost", DEFAULT.swingBoost).forGetter(RopeParams::swingBoost),
        Codec.FLOAT.optionalFieldOf("max_swing_speed", DEFAULT.maxSwingSpeed).forGetter(RopeParams::maxSwingSpeed),
        Codec.FLOAT.optionalFieldOf("control_accel", DEFAULT.controlAccel).forGetter(RopeParams::controlAccel),
        Codec.FLOAT.optionalFieldOf("reel_step", DEFAULT.reelStep).forGetter(RopeParams::reelStep),
        Codec.FLOAT.optionalFieldOf("slack_pull_rate", DEFAULT.slackPullRate).forGetter(RopeParams::slackPullRate),
        Codec.BOOL.optionalFieldOf("constrain_from", DEFAULT.constrainFrom).forGetter(RopeParams::constrainFrom),
        Codec.BOOL.optionalFieldOf("constrain_to", DEFAULT.constrainTo).forGetter(RopeParams::constrainTo),
        Codec.BOOL.optionalFieldOf("controllable", DEFAULT.controllable).forGetter(RopeParams::controllable),
        Mode.CODEC.optionalFieldOf("mode", DEFAULT.mode).forGetter(RopeParams::mode),
        Codec.FLOAT.optionalFieldOf("break_beyond", DEFAULT.breakBeyond).forGetter(RopeParams::breakBeyond)
    ).apply(i, RopeParams::new));
}
