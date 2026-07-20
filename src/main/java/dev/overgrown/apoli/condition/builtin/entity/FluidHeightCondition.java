package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class FluidHeightCondition implements ConditionType<EntityCtx, FluidHeightCondition.Cfg> {
    public record Cfg(ResourceLocation fluid, Comparison comparison, float compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(Cfg::fluid),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        TagKey<Fluid> tag = TagKey.create(Registries.FLUID, cfg.fluid);
        double h = ctx.raw().getFluidHeight(tag);
        return cfg.comparison.compare((float) h, cfg.compareTo);
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
