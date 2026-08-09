package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

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

        public BlockPos apply(BlockPos worldSpawn, int center, float multiplier) {
            if (this == CENTER) return new BlockPos(0, center, 0);
            if (multiplier == 0.0F) return worldSpawn;
            return new BlockPos(
                (int) (worldSpawn.getX() * multiplier),
                worldSpawn.getY(),
                (int) (worldSpawn.getZ() * multiplier));
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

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder.owner() instanceof ServerPlayer player)) return;
        if (player.getRespawnPosition() == null || !player.isRespawnForced()) return;
        if (!ResourceKey.create(Registries.DIMENSION, cfg.dimension()).equals(player.getRespawnDimension())) return;
        if (ModifyPlayerSpawnHandler.anyOtherActive(holder, powerId)) return;
        player.setRespawnPosition(Level.OVERWORLD, null, 0.0F, false, false);
    }
}
