package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class StatusBarTexturePower extends PowerType<StatusBarTexturePower.Config> {
    public record Config(
        Optional<ResourceLocation> texture,
        Optional<Map<String, ResourceLocation>> textureMap
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(Config::texture),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                .optionalFieldOf("texture_map").forGetter(Config::textureMap)
        ).apply(i, Config::new));
    }
}
