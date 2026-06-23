package dev.overgrown.apoli.condition.builtin.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class InTagCondition implements ConditionType<BiomeCtx, InTagCondition.Cfg> {
    public record Cfg(ResourceLocation tag) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("tag").forGetter(Cfg::tag)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiomeCtx ctx) {
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, cfg.tag);
        return ctx.biome().is(tag);
    }
}
