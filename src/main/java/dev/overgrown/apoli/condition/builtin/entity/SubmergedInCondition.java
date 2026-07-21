package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class SubmergedInCondition implements ConditionType<EntityCtx, SubmergedInCondition.Cfg> {
    public record Cfg(ResourceLocation fluid) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(Cfg::fluid)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        TagKey<Fluid> tag = TagKey.create(Registries.FLUID, cfg.fluid);
        return ctx.raw().isEyeInFluid(tag);
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
