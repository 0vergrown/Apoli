package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.entity.Pose;

import java.util.Locale;

public final class EntityPoses {
    private EntityPoses() {}

    public static final Codec<Pose> CODEC = Codec.STRING.comapFlatMap(
        EntityPoses::byName,
        pose -> pose.name().toLowerCase(Locale.ROOT));

    private static DataResult<Pose> byName(String name) {
        try {
            return DataResult.success(Pose.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown entity pose: '" + name + "'");
        }
    }
}
