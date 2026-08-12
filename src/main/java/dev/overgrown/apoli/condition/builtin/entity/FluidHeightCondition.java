package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class FluidHeightCondition implements ConditionType<EntityCtx, FluidHeightCondition.Cfg> {
    public record Cfg(TagKey<Fluid> fluid, Comparison comparison, float compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.<Fluid>tagKey(Registries.FLUID).fieldOf("fluid").forGetter(Cfg::fluid),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        double h = ctx.raw().getFluidHeight(cfg.fluid);
        return cfg.comparison.compare((float) h, cfg.compareTo);
    }
}
