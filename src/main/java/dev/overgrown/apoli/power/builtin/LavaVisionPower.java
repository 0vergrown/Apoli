package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class LavaVisionPower extends PowerType<LavaVisionPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("lava_vision");

    public record Config(
            Optional<Float> s,
            Optional<Float> v
    ){}
    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("v").forGetter(Config::v),
                Codec.FLOAT.optionalFieldOf("s").forGetter(Config::s)
        ).apply(i, LavaVisionPower.Config::new));
    }
}
