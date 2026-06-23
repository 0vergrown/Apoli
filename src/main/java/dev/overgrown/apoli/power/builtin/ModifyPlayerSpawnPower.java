package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public final class ModifyPlayerSpawnPower extends PowerType<ModifyPlayerSpawnPower.Config> {
    public record Config(
        ResourceLocation dimension,
        Optional<ResourceLocation> biome,
        Optional<ResourceLocation> structure,
        SpawnStrategy spawnStrategy,
        Optional<Float> dimensionDistanceMultiplier
    ) {}

    public enum SpawnStrategy implements StringRepresentable {
        DEFAULT("default"),
        CENTER("center");

        public static final Codec<SpawnStrategy> CODEC = StringRepresentable.fromEnum(SpawnStrategy::values);

        private final String name;
        SpawnStrategy(String name) {
            this.name = name;
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(Config::dimension),
            ResourceLocation.CODEC.optionalFieldOf("biome").forGetter(Config::biome),
            ResourceLocation.CODEC.optionalFieldOf("structure").forGetter(Config::structure),
            SpawnStrategy.CODEC.optionalFieldOf("spawn_strategy", SpawnStrategy.DEFAULT).forGetter(Config::spawnStrategy),
            Codec.FLOAT.optionalFieldOf("dimension_distance_multiplier").forGetter(Config::dimensionDistanceMultiplier)
        ).apply(i, Config::new));
    }
}
