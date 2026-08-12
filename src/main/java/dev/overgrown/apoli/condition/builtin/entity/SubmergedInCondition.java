package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class SubmergedInCondition implements ConditionType<EntityCtx, SubmergedInCondition.Cfg> {
    public record Cfg(TagKey<Fluid> fluid) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.<Fluid>tagKey(Registries.FLUID).fieldOf("fluid").forGetter(Cfg::fluid)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return ctx.raw().isEyeInFluid(cfg.fluid);
    }
}
