package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiomeCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

public final class ModifyPlayerSpawnPower extends PowerType<ModifyPlayerSpawnPower.Config> {
    private static final int DEFAULT_SEARCH_RADIUS = 32;
    private static final int MAX_SEARCH_RADIUS = 64;
    private static final Codec<Integer> SEARCH_RADIUS = Codec.INT.flatXmap(ModifyPlayerSpawnPower::checkRadius, DataResult::success);

    public record Config(
        ResourceLocation dimension,
        Optional<ResourceLocation> biome,
        Optional<BiomeCondition> biomeCondition,
        Optional<ResourceLocation> structure,
        Optional<TagKey<Structure>> structureTag,
        Optional<BlockCondition> blockCondition,
        SpawnStrategy spawnStrategy,
        Optional<Float> dimensionDistanceMultiplier,
        int searchRadius
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
            IdCodecs.ID.fieldOf("dimension").forGetter(Config::dimension),
            IdCodecs.ID.optionalFieldOf("biome").forGetter(Config::biome),
            LoggedOptionalField.of("biome_condition", BiomeCondition.CODEC).forGetter(Config::biomeCondition),
            IdCodecs.ID.optionalFieldOf("structure").forGetter(Config::structure),
            IdCodecs.tagKey(Registries.STRUCTURE).optionalFieldOf("structure_tag").forGetter(Config::structureTag),
            LoggedOptionalField.of("block_condition", BlockCondition.CODEC).forGetter(Config::blockCondition),
            SpawnStrategy.CODEC.optionalFieldOf("spawn_strategy", SpawnStrategy.DEFAULT).forGetter(Config::spawnStrategy),
            Codec.FLOAT.optionalFieldOf("dimension_distance_multiplier").forGetter(Config::dimensionDistanceMultiplier),
            SEARCH_RADIUS.optionalFieldOf("search_radius", DEFAULT_SEARCH_RADIUS).forGetter(Config::searchRadius)
        ).apply(i, Config::new));
    }

    private static DataResult<Integer> checkRadius(int radius) {
        if (radius >= 1 && radius <= MAX_SEARCH_RADIUS) return DataResult.success(radius);
        int clamped = Mth.clamp(radius, 1, MAX_SEARCH_RADIUS);
        return DataResult.error(() -> "search_radius " + radius + " is outside 1 to " + MAX_SEARCH_RADIUS
            + ", so " + clamped + " is used", clamped);
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
