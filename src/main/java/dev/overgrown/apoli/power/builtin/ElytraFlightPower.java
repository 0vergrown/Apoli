package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ElytraFlightPower extends PowerType<ElytraFlightPower.Config> {
    public record Config(boolean renderElytra, Optional<ResourceLocation> textureLocation) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("render_elytra").forGetter(Config::renderElytra),
            ResourceLocation.CODEC.optionalFieldOf("texture_location").forGetter(Config::textureLocation)
        ).apply(i, Config::new));
    }
}
