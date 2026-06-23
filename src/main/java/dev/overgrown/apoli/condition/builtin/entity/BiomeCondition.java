package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Optional;

public final class BiomeCondition implements ConditionType<EntityCtx, BiomeCondition.Cfg> {
    public record Cfg(
        Optional<ResourceLocation> biome,
        Optional<List<ResourceLocation>> biomes,
        Optional<dev.overgrown.apoli.condition.BiomeCondition> condition
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("biome").forGetter(Cfg::biome),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("biomes").forGetter(Cfg::biomes),
            dev.overgrown.apoli.condition.BiomeCondition.CODEC.optionalFieldOf("condition").forGetter(Cfg::condition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        BlockPos pos = ctx.entity().blockPosition();
        Holder<Biome> here = ctx.level().getBiome(pos);
        ResourceLocation id = here.unwrapKey().map(k -> k.location()).orElse(null);

        boolean noFilters = cfg.biome.isEmpty() && cfg.biomes.isEmpty() && cfg.condition.isEmpty();
        if (noFilters) return true;
        if (id != null && cfg.biome.isPresent() && cfg.biome.get().equals(id)) return true;
        if (id != null && cfg.biomes.isPresent() && cfg.biomes.get().contains(id)) return true;
        if (cfg.condition.isPresent()) {
            return cfg.condition.get().test(new BiomeCtx(here, pos, ctx.level()));
        }
        return false;
    }
}
