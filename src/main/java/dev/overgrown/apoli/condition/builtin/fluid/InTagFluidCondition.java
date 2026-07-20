package dev.overgrown.apoli.condition.builtin.fluid;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.FluidCtx;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class InTagFluidCondition implements ConditionType<FluidCtx, InTagFluidCondition.Cfg> {
    public record Cfg(ResourceLocation tag) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("tag").forGetter(Cfg::tag)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, FluidCtx ctx) {
        TagKey<Fluid> key = TagKey.create(Registries.FLUID, cfg.tag);
        return ctx.state().is(key);
    }
}
