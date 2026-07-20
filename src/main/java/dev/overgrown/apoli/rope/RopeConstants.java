package dev.overgrown.apoli.rope;

import net.minecraft.world.phys.Vec3;

public final class RopeConstants {
    private RopeConstants() {}

    public static final float MIN_ROPE_LENGTH = 1.0f;
    public static final float GOAL_ROPE_SEGMENT_LENGTH = 0.5f;
    public static final float ROPE_LENGTH_CHANGE_STEP = 0.2f;
    public static final float SLACK_PULL_RATE_MULT = 5;

    public static final float RADIAL_DAMPING = 0.85f;
    public static final float LEASH_STIFFNESS = 0.1f;
    public static final float SPRING_SCALING = 0.65f;
    public static final float SWING_BOOST = 1.08f;
    public static final float MAX_SWING_SPEED = 0.7f;
    public static final float SWING_CONTROL_ACCEL = 0.08f;
    public static final float ELYTRA_LENGTH_MOD = 5;
    public static final int ELYTRA_TIME_LIMIT = 10;

    public static final Vec3 GRAVITY = new Vec3(0, -0.08, 0);
    public static final float ROPE_DAMPING = 0.99f;
    public static final float ROPE_STIFFNESS = 0.98f;

    public static final float ROPE_WIDTH = 0.1f;
}
