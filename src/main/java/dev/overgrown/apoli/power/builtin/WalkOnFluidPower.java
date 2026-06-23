package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class WalkOnFluidPower extends PowerType<WalkOnFluidPower.Config> {
    public record Config(ResourceLocation fluid) {
        public TagKey<Fluid> fluidTag() {
            return TagKey.create(Registries.FLUID, fluid);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(Config::fluid)
        ).apply(i, Config::new));
    }
}
