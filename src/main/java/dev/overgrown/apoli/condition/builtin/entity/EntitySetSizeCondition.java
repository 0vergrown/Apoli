package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.EntitySetPower;
import net.minecraft.resources.ResourceLocation;

public final class EntitySetSizeCondition implements ConditionType<EntityCtx, EntitySetSizeCondition.Cfg> {
    public record Cfg(ResourceLocation set, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("set").forGetter(Cfg::set),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        PowerContainer container = PowerContainer.of(ctx.entity());
        if (container == null || !container.hasPower(cfg.set)) return false;
        if (EntitySetPower.resolveCfg(cfg.set) == null) return false;
        int size = EntitySetPower.size(ctx.entity(), cfg.set);
        return cfg.comparison.compare(size, cfg.compareTo);
    }
}
