package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public enum Space implements StringRepresentable {
    WORLD("world"),
    LOCAL("local"),
    LOCAL_HORIZONTAL("local_horizontal"),
    LOCAL_HORIZONTAL_NORMALIZED("local_horizontal_normalized"),
    VELOCITY("velocity"),
    VELOCITY_NORMALIZED("velocity_normalized"),
    VELOCITY_HORIZONTAL("velocity_horizontal"),
    VELOCITY_HORIZONTAL_NORMALIZED("velocity_horizontal_normalized");

    public static final Codec<Space> CODEC = StringRepresentable.fromEnum(Space::values);

    private final String name;

    Space(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Vec3 toGlobal(Entity entity, Vec3 input) {
        Vec3 forward;
        boolean normalize;
        switch (this) {
            case WORLD -> { return input; }
            case LOCAL -> { forward = entity.getLookAngle(); normalize = false; }
            case LOCAL_HORIZONTAL -> { forward = horizontal(entity.getLookAngle()); normalize = false; }
            case LOCAL_HORIZONTAL_NORMALIZED -> { forward = horizontal(entity.getLookAngle()); normalize = true; }
            case VELOCITY -> { forward = entity.getDeltaMovement(); normalize = false; }
            case VELOCITY_NORMALIZED -> { forward = entity.getDeltaMovement(); normalize = true; }
            case VELOCITY_HORIZONTAL -> { forward = horizontal(entity.getDeltaMovement()); normalize = false; }
            case VELOCITY_HORIZONTAL_NORMALIZED -> { forward = horizontal(entity.getDeltaMovement()); normalize = true; }
            default -> { return input; }
        }
        return transformVectorToBase(forward, input, entity.getYRot(), normalize);
    }

    private static Vec3 horizontal(Vec3 v) {
        return new Vec3(v.x, 0.0, v.z);
    }

    public static Vec3 transformVectorToBase(Vec3 baseForward, Vec3 input, float baseYaw, boolean normalize) {
        double baseScale = baseForward.length();
        if (baseScale <= 0.007) return Vec3.ZERO;

        Vec3 z = baseForward.scale(1.0 / baseScale);
        double xX, xZ;
        if (Math.abs(z.y) != 1.0) {
            xX = z.z;
            xZ = -z.x;
            double f = 1.0 / Math.sqrt(xX * xX + xZ * xZ);
            xX *= f;
            xZ *= f;
        } else {
            double trigYaw = -baseYaw * (Math.PI / 180.0);
            xX = Math.cos(trigYaw);
            xZ = -Math.sin(trigYaw);
        }
        double yX = z.y * xZ;
        double yY = z.z * xX - z.x * xZ;
        double yZ = -z.y * xX;

        double scale = normalize ? 1.0 : baseScale;
        return new Vec3(
            (xX * input.x + yX * input.y + z.x * input.z) * scale,
            (yY * input.y + z.y * input.z) * scale,
            (xZ * input.x + yZ * input.y + z.z * input.z) * scale
        );
    }
}
