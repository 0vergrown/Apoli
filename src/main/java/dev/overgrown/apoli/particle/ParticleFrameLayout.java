package dev.overgrown.apoli.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.overgrown.apoli.data.ModelParts;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum ParticleFrameLayout {
    AUTO,
    VERTICAL,
    HORIZONTAL,
    GRID;

    private static final Map<String, ParticleFrameLayout> BY_NAME = new HashMap<>();

    static {
        for (ParticleFrameLayout layout : values()) BY_NAME.put(ModelParts.normalize(layout.name()), layout);
        BY_NAME.put("strip", VERTICAL);
        BY_NAME.put("column", VERTICAL);
        BY_NAME.put("row", HORIZONTAL);
        BY_NAME.put("sheet", GRID);
        BY_NAME.put("atlas", GRID);
    }

    public static final Codec<ParticleFrameLayout> CODEC = Codec.STRING.comapFlatMap(
        string -> {
            ParticleFrameLayout layout = BY_NAME.get(ModelParts.normalize(string));
            return layout != null
                ? DataResult.success(layout)
                : DataResult.error(() -> "Unknown particle frame layout: '" + string
                    + "' (expected auto, vertical, horizontal or grid)");
        },
        layout -> layout.name().toLowerCase(Locale.ROOT)
    );
}
