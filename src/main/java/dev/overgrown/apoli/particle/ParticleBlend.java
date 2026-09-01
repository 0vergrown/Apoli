package dev.overgrown.apoli.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.data.ModelParts;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum ParticleBlend {
    TRANSLUCENT,
    ADDITIVE;

    private static final Map<String, ParticleBlend> BY_NAME = new HashMap<>();

    static {
        for (ParticleBlend blend : values()) BY_NAME.put(ModelParts.normalize(blend.name()), blend);
        BY_NAME.put("alpha", TRANSLUCENT);
        BY_NAME.put("glow", ADDITIVE);
    }

    public static final Codec<ParticleBlend> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            ParticleBlend blend = BY_NAME.get(ModelParts.normalize(string));
            return blend != null
                ? DataResult.success(blend)
                : DataResult.error(() -> "Unknown particle blend: '" + string + "' (expected translucent or additive)");
        },
        blend -> blend.name().toLowerCase(Locale.ROOT)
    );
}
