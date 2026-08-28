package dev.overgrown.apoli.mount;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public enum MountRotation {
    HEAD("head"),
    BODY("body");

    public static final Codec<MountRotation> CODEC = Codec.STRING.comapFlatMap(
        MountRotation::byName, MountRotation::getSerializedName);

    private final String name;

    MountRotation(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return name;
    }

    public float yawOf(Entity vehicle) {
        if (this == BODY && vehicle instanceof LivingEntity living) return living.yBodyRot;
        return vehicle.getYRot();
    }

    public float yawOf(Entity vehicle, float partialTick) {
        if (this == BODY && vehicle instanceof LivingEntity living) {
            return Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        }
        return Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot());
    }

    private static DataResult<MountRotation> byName(String raw) {
        return switch (raw) {
            case "head", "HEAD" -> DataResult.success(HEAD);
            case "body", "BODY" -> DataResult.success(BODY);
            default -> DataResult.error(() -> "'" + raw
                + "' is not a mount rotation. Use \"head\" (the vehicle's look direction) or \"body\" "
                + "(the vehicle's body yaw, so a rider stays put when the vehicle looks around).");
        };
    }
}
