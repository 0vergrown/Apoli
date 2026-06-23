package dev.overgrown.apoli.condition.builtin.biome;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class HighHumidityCondition implements ConditionType<BiomeCtx, EmptyCfg> {
    private static final TagKey<Biome> INCREASED_FIRE_BURNOUT = TagKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "increased_fire_burnout"));

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BiomeCtx ctx) {
        return ctx.biome().is(INCREASED_FIRE_BURNOUT);
    }
}
