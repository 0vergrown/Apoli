package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalInt;

public final class ResourceCondition implements ConditionType<EntityCtx, ResourceCondition.Cfg> {
    public record Cfg(ResourceLocation resource, Comparison comparison, Expression compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("resource").forGetter(Cfg::resource),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Expression.INT_OR_EXPR.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null) return false;
        OptionalInt value = PowerResources.read(container, cfg.resource);
        if (value.isEmpty()) return false;
        int rhs = cfg.compareTo.evalIntWith(ctx.entity(), container, value.getAsInt());
        return cfg.comparison.compare(value.getAsInt(), rhs);
    }
}
