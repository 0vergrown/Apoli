package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

public final class ShaderPower extends PowerType<ShaderPower.Config> {
    public record Config(ResourceLocation shader, boolean toggleable) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("shader").forGetter(Config::shader),
            Codec.BOOL.optionalFieldOf("toggleable", true).forGetter(Config::toggleable)
        ).apply(i, Config::new));
    }
}
