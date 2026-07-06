package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModelColorPower extends PowerType<ModelColorPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("model_color");

    public record PartColor(String part, float red, float green, float blue, float alpha) {}

    public record Config(float red, float green, float blue, float alpha, List<PartColor> parts) {
        public boolean hasParts() {
            return !parts.isEmpty();
        }
    }

    public static final float[] IDENTITY = new float[]{1f, 1f, 1f, 1f};

    private static final MapCodec<PartColor> PART_COLOR_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("part").forGetter(PartColor::part),
        Codec.FLOAT.optionalFieldOf("red", 1f).forGetter(PartColor::red),
        Codec.FLOAT.optionalFieldOf("green", 1f).forGetter(PartColor::green),
        Codec.FLOAT.optionalFieldOf("blue", 1f).forGetter(PartColor::blue),
        Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(PartColor::alpha)
    ).apply(i, PartColor::new));

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("red", 1f).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1f).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1f).forGetter(Config::blue),
            Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(Config::alpha),
            PART_COLOR_CODEC.codec().listOf().optionalFieldOf("parts", List.of()).forGetter(Config::parts)
        ).apply(i, Config::new));
    }

    public static float[] colorFor(LivingEntity entity) {
        float[] rgba = new float[]{1f, 1f, 1f, 1f};
        boolean[] any = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.hasParts()) return;
            any[0] = true;
            rgba[0] *= cfg.red;
            rgba[1] *= cfg.green;
            rgba[2] *= cfg.blue;
            rgba[3] *= cfg.alpha;
        });
        return any[0] ? rgba : IDENTITY;
    }

    public static boolean hasPartColors(LivingEntity entity) {
        boolean[] any = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.hasParts()) any[0] = true;
        });
        return any[0];
    }

    @Nullable
    public static Map<String, float[]> partColorsFor(LivingEntity entity) {
        Map<String, float[]> map = new HashMap<>();
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            for (PartColor pc : cfg.parts()) {
                float[] c = map.computeIfAbsent(ModelParts.normalize(pc.part()), k -> new float[]{1f, 1f, 1f, 1f});
                c[0] *= pc.red();
                c[1] *= pc.green();
                c[2] *= pc.blue();
                c[3] *= pc.alpha();
            }
        });
        return map.isEmpty() ? null : map;
    }

    public static float minAlpha(LivingEntity entity) {
        float min = colorFor(entity)[3];
        if (hasPartColors(entity)) {
            Map<String, float[]> parts = partColorsFor(entity);
            if (parts != null) {
                for (float[] c : parts.values()) {
                    min = Math.min(min, c[3]);
                }
            }
        }
        return min;
    }
}
