package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ModelParts;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModelColorPower extends PowerType<ModelColorPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("model_color");

    public record PartColor(String part, float red, float green, float blue, float alpha,
                            boolean whiten, Optional<EntityCondition> condition) {}

    public record Config(float red, float green, float blue, float alpha, boolean whiten, List<PartColor> parts) {
        public boolean hasParts() {
            return !parts.isEmpty();
        }
    }

    public static final float[] IDENTITY = new float[]{1f, 1f, 1f, 1f, 0f};

    private static boolean explicitWhite(Optional<Float> r, Optional<Float> g, Optional<Float> b) {
        return r.isPresent() && g.isPresent() && b.isPresent()
            && r.get() >= 0.999f && g.get() >= 0.999f && b.get() >= 0.999f;
    }

    private static final MapCodec<PartColor> PART_COLOR_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("part").forGetter(PartColor::part),
        Codec.FLOAT.optionalFieldOf("red").forGetter(pc -> Optional.of(pc.red())),
        Codec.FLOAT.optionalFieldOf("green").forGetter(pc -> Optional.of(pc.green())),
        Codec.FLOAT.optionalFieldOf("blue").forGetter(pc -> Optional.of(pc.blue())),
        Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(PartColor::alpha),
        EntityCondition.CODEC.optionalFieldOf("condition").forGetter(PartColor::condition)
    ).apply(i, (part, r, g, b, alpha, condition) -> new PartColor(
        part, r.orElse(1f), g.orElse(1f), b.orElse(1f), alpha, explicitWhite(r, g, b), condition)));

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("red").forGetter(c -> Optional.of(c.red())),
            Codec.FLOAT.optionalFieldOf("green").forGetter(c -> Optional.of(c.green())),
            Codec.FLOAT.optionalFieldOf("blue").forGetter(c -> Optional.of(c.blue())),
            Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(Config::alpha),
            PART_COLOR_CODEC.codec().listOf().optionalFieldOf("parts", List.of()).forGetter(Config::parts)
        ).apply(i, (r, g, b, alpha, parts) -> new Config(
            r.orElse(1f), g.orElse(1f), b.orElse(1f), alpha, explicitWhite(r, g, b), parts)));
    }

    public static float[] colorFor(Entity entity) {
        float[] rgba = new float[]{1f, 1f, 1f, 1f, 0f};
        boolean[] any = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.hasParts()) return;
            any[0] = true;
            rgba[0] *= cfg.red;
            rgba[1] *= cfg.green;
            rgba[2] *= cfg.blue;
            rgba[3] *= cfg.alpha;
            if (cfg.whiten()) rgba[4] = 1f;
        });
        return any[0] ? rgba : IDENTITY;
    }

    public static boolean hasPartColors(Entity entity) {
        boolean[] any = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (cfg.hasParts()) any[0] = true;
        });
        return any[0];
    }

    @Nullable
    public static Map<String, float[]> partColorsFor(Entity entity) {
        Map<String, float[]> map = new HashMap<>();

        EntityCtx[] ctx = new EntityCtx[1];
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            for (PartColor pc : cfg.parts()) {
                if (pc.condition().isPresent()) {
                    if (ctx[0] == null) ctx[0] = EntityCtx.of(entity, entity.level());
                    if (!pc.condition().get().test(ctx[0])) continue;
                }
                float[] c = map.computeIfAbsent(ModelParts.normalize(pc.part()), k -> new float[]{1f, 1f, 1f, 1f, 0f});
                c[0] *= pc.red();
                c[1] *= pc.green();
                c[2] *= pc.blue();
                c[3] *= pc.alpha();
                if (pc.whiten()) c[4] = 1f;
            }
        });
        return map.isEmpty() ? null : map;
    }

    public static float minAlpha(Entity entity) {
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
