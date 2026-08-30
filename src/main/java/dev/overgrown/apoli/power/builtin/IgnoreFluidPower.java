package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.FluidCondition;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;

public final class IgnoreFluidPower extends PowerType<IgnoreFluidPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("ignore_fluid");

    public record Config(FluidCondition fluidCondition) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            FluidCondition.CODEC.fieldOf("fluid_condition").forGetter(Config::fluidCondition)
        ).apply(i, Config::new));
    }

    public static boolean ignores(Entity entity, FluidState fluid, BlockPos pos) {
        FluidCtx ctx = new FluidCtx(fluid, pos, entity.level());
        boolean[] match = new boolean[]{false};
        PowerLookup.forEach(entity, CANONICAL, Config.class, cfg -> {
            if (match[0]) return;
            if (cfg.fluidCondition.test(ctx)) match[0] = true;
        });
        return match[0];
    }
}
