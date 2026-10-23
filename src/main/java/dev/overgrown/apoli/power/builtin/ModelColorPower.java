package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.BodyPart;
import dev.overgrown.apoli.data.Expression;
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

    public record Tint(Expression red, Expression green, Expression blue, Expression alpha, boolean explicitRgb) {
        private static final Expression ONE = Expression.constant(1);

        public static final MapCodec<Tint> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.FLOAT_OR_EXPR.optionalFieldOf("red").forGetter(t -> t.channel(t.red())),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("green").forGetter(t -> t.channel(t.green())),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("blue").forGetter(t -> t.channel(t.blue())),
            Expression.FLOAT_OR_EXPR.optionalFieldOf("alpha", ONE).forGetter(Tint::alpha)
        ).apply(i, (r, g, b, alpha) -> new Tint(r.orElse(ONE), g.orElse(ONE), b.orElse(ONE), alpha,
            r.isPresent() && g.isPresent() && b.isPresent())));

        private Optional<Expression> channel(Expression value) {
            boolean one = value.constantValue().isPresent() && value.constantValue().getAsDouble() == 1.0;
            return explicitRgb || !one ? Optional.of(value) : Optional.empty();
        }

        public float red(@Nullable Entity entity) {
            return (float) red.eval(entity);
        }

        public float green(@Nullable Entity entity) {
            return (float) green.eval(entity);
        }

        public float blue(@Nullable Entity entity) {
            return (float) blue.eval(entity);
        }

        public float alpha(@Nullable Entity entity) {
            return (float) alpha.eval(entity);
        }

        public boolean whitens(float r, float g, float b) {
            return explicitRgb && r >= 0.999f && g >= 0.999f && b >= 0.999f;
        }

        public void resolve(@Nullable Entity entity, float[] out) {
            float r = red(entity);
            float g = green(entity);
            float b = blue(entity);
            out[0] = r;
            out[1] = g;
            out[2] = b;
            out[3] = alpha(entity);
            out[4] = whitens(r, g, b) ? 1f : 0f;
        }

        public void applyTo(@Nullable Entity entity, float[] rgba) {
            float r = red(entity);
            float g = green(entity);
            float b = blue(entity);
            rgba[0] *= r;
            rgba[1] *= g;
            rgba[2] *= b;
            rgba[3] *= alpha(entity);
            if (whitens(r, g, b)) rgba[4] = 1f;
        }
    }

    public record PartColor(BodyPart part, Tint tint, Optional<EntityCondition> condition) {}

    public record Config(Tint tint, List<PartColor> parts) {
        public boolean hasParts() {
            return !parts.isEmpty();
        }
    }

    public static final float[] IDENTITY = new float[]{1f, 1f, 1f, 1f, 0f};

    private static final ThreadLocal<List<Config>> CONFIGS = ThreadLocal.withInitial(() -> new ArrayList<>(2));

    private static final MapCodec<PartColor> PART_COLOR_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BodyPart.CODEC.fieldOf("part").forGetter(PartColor::part),
        Tint.MAP_CODEC.forGetter(PartColor::tint),
        dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(PartColor::condition)
    ).apply(i, PartColor::new));

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Tint.MAP_CODEC.forGetter(Config::tint),
            PART_COLOR_CODEC.codec().listOf().optionalFieldOf("parts", List.of()).forGetter(Config::parts)
        ).apply(i, Config::new));
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
            cfg.tint().applyTo(entity, rgba);
        }
        configs.clear();
        return rgba == null ? IDENTITY : rgba;
    }

    public static boolean resolve(Entity entity, float[] whole, List<PartColor> parts) {
        whole[0] = 1f;
        whole[1] = 1f;
        whole[2] = 1f;
        whole[3] = 1f;
        whole[4] = 0f;
        List<Config> configs = CONFIGS.get();
        configs.clear();
        PowerLookup.collect(entity, CANONICAL, Config.class, configs);
        if (configs.isEmpty()) return false;
        EntityCtx ctx = null;
        for (int i = 0; i < configs.size(); i++) {
            Config cfg = configs.get(i);
            if (!cfg.hasParts()) {
                cfg.tint().applyTo(entity, whole);
                continue;
            }
            List<PartColor> partList = cfg.parts();
            for (int p = 0; p < partList.size(); p++) {
                PartColor pc = partList.get(p);
                if (pc.condition().isPresent()) {
                    if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                    if (!pc.condition().get().test(ctx)) continue;
                }
                parts.add(pc);
            }
        }
        configs.clear();
        return true;
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
            min = Math.min(min, parts.get(i).tint().alpha(entity));
        }
        parts.clear();
        return min;
    }

    private static final ThreadLocal<List<PartColor>> PART_COLORS = ThreadLocal.withInitial(() -> new ArrayList<>(4));
}
