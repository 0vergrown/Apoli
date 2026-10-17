package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class StatusBarTexturePower extends PowerType<StatusBarTexturePower.Config> {
    public record Config(Optional<ResourceLocation> texture, Map<ResourceLocation, ResourceLocation> textureMap, int priority) {
        public Config {
            textureMap = Map.copyOf(textureMap);
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.<Config>mapCodec(i -> i.group(
            IdCodecs.ID.optionalFieldOf("texture").forGetter(Config::texture),
            Codec.unboundedMap(IdCodecs.ID, IdCodecs.ID).optionalFieldOf("texture_map", Map.of()).forGetter(Config::textureMap),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new)).flatXmap(StatusBarTexturePower::requireTexture, DataResult::success);
    }

    private static DataResult<Config> requireTexture(Config cfg) {
        if (cfg.texture.isPresent() || !cfg.textureMap.isEmpty()) return DataResult.success(cfg);
        return DataResult.error(() -> "apoli:status_bar_texture needs a 'texture' sprite sheet, a 'texture_map', or both");
    }
}
