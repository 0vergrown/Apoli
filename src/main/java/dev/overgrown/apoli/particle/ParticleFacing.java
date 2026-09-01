package dev.overgrown.apoli.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.data.ModelParts;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum ParticleFacing {
    CAMERA,
    VERTICAL;

    private static final Map<String, ParticleFacing> BY_NAME = new HashMap<>();

    static {
        for (ParticleFacing facing : values()) BY_NAME.put(ModelParts.normalize(facing.name()), facing);
        BY_NAME.put("billboard", CAMERA);
        BY_NAME.put("upright", VERTICAL);
    }

    public static final Codec<ParticleFacing> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            ParticleFacing facing = BY_NAME.get(ModelParts.normalize(string));
            return facing != null
                ? DataResult.success(facing)
                : DataResult.error(() -> "Unknown particle facing: '" + string + "' (expected camera or vertical)");
        },
        facing -> facing.name().toLowerCase(Locale.ROOT)
    );
}
