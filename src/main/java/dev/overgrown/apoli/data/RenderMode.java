package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum RenderMode {
    TRANSLUCENT,
    TRANSLUCENT_CULL,
    CUTOUT,
    CUTOUT_NO_CULL,
    SOLID,
    EMISSIVE,
    EYES;

    private static final Map<String, RenderMode> BY_NAME = new HashMap<>();

    static {
        for (RenderMode mode : values()) {
            BY_NAME.put(ModelParts.normalize(mode.name()), mode);
        }
        BY_NAME.put(ModelParts.normalize("glow"), EMISSIVE);
        BY_NAME.put(ModelParts.normalize("translucent_emissive"), EMISSIVE);
        BY_NAME.put(ModelParts.normalize("glowing"), EMISSIVE);
        BY_NAME.put(ModelParts.normalize("eyes_emissive"), EYES);
    }

    public static final Codec<RenderMode> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            RenderMode mode = BY_NAME.get(ModelParts.normalize(string));
            return mode != null
                ? DataResult.success(mode)
                : DataResult.error(() -> "Unknown render type: '" + string + "'");
        },
        mode -> mode.name().toLowerCase(Locale.ROOT)
    );
}
