package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModelColorPower extends PowerType<ModelColorPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("model_color");

    public record PartColor(BodyPart part, float red, float green, float blue, float alpha,
                            boolean whiten, Optional<EntityCondition> condition) {}

    public record Config(float red, float green, float blue, float alpha, boolean whiten, List<PartColor> parts) {
        public boolean hasParts() {
            return !parts.isEmpty();
        }
    }

    public static final float[] IDENTITY = new float[]{1f, 1f, 1f, 1f, 0f};

    private static final ThreadLocal<List<Config>> CONFIGS = ThreadLocal.withInitial(() -> new ArrayList<>(2));

    private static Optional<Float> channel(float value, boolean whiten) {
        return whiten || value != 1f ? Optional.of(value) : Optional.empty();
    }

    private static boolean explicitWhite(Optional<Float> r, Optional<Float> g, Optional<Float> b) {
        return r.isPresent() && g.isPresent() && b.isPresent()
            && r.get() >= 0.999f && g.get() >= 0.999f && b.get() >= 0.999f;
    }

    private static final MapCodec<PartColor> PART_COLOR_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BodyPart.CODEC.fieldOf("part").forGetter(PartColor::part),
        Codec.FLOAT.optionalFieldOf("red").forGetter(pc -> channel(pc.red(), pc.whiten())),
        Codec.FLOAT.optionalFieldOf("green").forGetter(pc -> channel(pc.green(), pc.whiten())),
        Codec.FLOAT.optionalFieldOf("blue").forGetter(pc -> channel(pc.blue(), pc.whiten())),
        Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(PartColor::alpha),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(PartColor::condition)
    ).apply(i, (part, r, g, b, alpha, condition) -> new PartColor(
        part, r.orElse(1f), g.orElse(1f), b.orElse(1f), alpha, explicitWhite(r, g, b), condition)));

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("red").forGetter(c -> channel(c.red(), c.whiten())),
            Codec.FLOAT.optionalFieldOf("green").forGetter(c -> channel(c.green(), c.whiten())),
            Codec.FLOAT.optionalFieldOf("blue").forGetter(c -> channel(c.blue(), c.whiten())),
            Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(Config::alpha),
            PART_COLOR_CODEC.codec().listOf().optionalFieldOf("parts", List.of()).forGetter(Config::parts)
        ).apply(i, (r, g, b, alpha, parts) -> new Config(
            r.orElse(1f), g.orElse(1f), b.orElse(1f), alpha, explicitWhite(r, g, b), parts)));
    }

    public static float[] colorFor(@Nullable Entity entity) {
        List<Config> configs = CONFIGS.get();
        configs.clear();
        PowerLookup.collect(entity, CANONICAL, Config.class, configs);
        float[] rgba = null;
        for (int i = 0; i < configs.size(); i++) {
            Config cfg = configs.get(i);
            if (cfg.hasParts()) continue;
            if (rgba == null) rgba = new float[]{1f, 1f, 1f, 1f, 0f};
            rgba[0] *= cfg.red();
            rgba[1] *= cfg.green();
            rgba[2] *= cfg.blue();
            rgba[3] *= cfg.alpha();
            if (cfg.whiten()) rgba[4] = 1f;
        }
        configs.clear();
        return rgba == null ? IDENTITY : rgba;
    }

    public static boolean hasPartColors(@Nullable Entity entity) {
        return PowerLookup.anyActive(entity, CANONICAL, Config.class, Config::hasParts);
    }

    public static void collectPartColors(@Nullable Entity entity, List<PartColor> out) {
        if (entity == null) return;
        List<Config> configs = CONFIGS.get();
        configs.clear();
        PowerLookup.collect(entity, CANONICAL, Config.class, configs);
        EntityCtx ctx = null;
        for (int i = 0; i < configs.size(); i++) {
            List<PartColor> parts = configs.get(i).parts();
            for (int p = 0; p < parts.size(); p++) {
                PartColor pc = parts.get(p);
                if (pc.condition().isPresent()) {
                    if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                    if (!pc.condition().get().test(ctx)) continue;
                }
                out.add(pc);
            }
        }
        configs.clear();
    }

    public static float minAlpha(@Nullable Entity entity) {
        float min = colorFor(entity)[3];
        if (!hasPartColors(entity)) return min;
        List<PartColor> parts = PART_COLORS.get();
        parts.clear();
        collectPartColors(entity, parts);
        for (int i = 0; i < parts.size(); i++) {
            min = Math.min(min, parts.get(i).alpha());
        }
        parts.clear();
        return min;
    }

    private static final ThreadLocal<List<PartColor>> PART_COLORS = ThreadLocal.withInitial(() -> new ArrayList<>(4));
}
