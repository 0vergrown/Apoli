package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.FluidCondition;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ModifyFluidRenderPower extends PowerType<ModifyFluidRenderPower.Config> {
    public record Config(
        Optional<BlockCondition> blockCondition,
        Optional<FluidCondition> fluidCondition,
        ResourceLocation fluid
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.optionalFieldOf("block_condition").forGetter(Config::blockCondition),
            FluidCondition.CODEC.optionalFieldOf("fluid_condition").forGetter(Config::fluidCondition),
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(Config::fluid)
        ).apply(i, Config::new));
    }
}
