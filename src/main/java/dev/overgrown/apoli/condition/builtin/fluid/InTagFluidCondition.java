package dev.overgrown.apoli.condition.builtin.fluid;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class InTagFluidCondition implements ConditionType<FluidCtx, InTagFluidCondition.Cfg> {
    public record Cfg(TagKey<Fluid> tag) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.<Fluid>tagKey(Registries.FLUID).fieldOf("tag").forGetter(Cfg::tag)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, FluidCtx ctx) {
        return ctx.state().is(cfg.tag);
    }
}
