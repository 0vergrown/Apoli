package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class ResourceCondition implements ConditionType<EntityCtx, ResourceCondition.Cfg> {
    public record Cfg(ResourceLocation resource, Optional<Expression> position,
                      Comparison comparison, Expression compareTo) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource),
        Expression.INT_OR_EXPR.optionalFieldOf("position").forGetter(Cfg::position),
        Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
        Expression.INT_OR_EXPR.fieldOf("compare_to").forGetter(Cfg::compareTo)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(INNER, Map.of(
            "index", "position",
            "slot", "position",
            "check", "compare_to"));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null) return false;
        if (cfg.position.isPresent()) {
            int slot = cfg.position.get().evalIntWith(ctx.entity(), container, 0);
            OptionalInt value = PowerResources.readAt(container, cfg.resource, slot);
            return value.isPresent() && matches(cfg, ctx, container, value.getAsInt());
        }
        int size = PowerResources.size(container, cfg.resource);
        if (size <= 1) {
            OptionalInt value = PowerResources.read(container, cfg.resource);
            return value.isPresent() && matches(cfg, ctx, container, value.getAsInt());
        }
        for (int slot = 0; slot < size; slot++) {
            OptionalInt value = PowerResources.readAt(container, cfg.resource, slot);
            if (value.isPresent() && matches(cfg, ctx, container, value.getAsInt())) return true;
        }
        return false;
    }

    private static boolean matches(Cfg cfg, EntityCtx ctx, PowerContainer container, int value) {
        int rhs = cfg.compareTo.evalIntWith(ctx.entity(), container, value);
        return cfg.comparison.compare(value, rhs);
    }
}
