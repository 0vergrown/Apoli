package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public final class PredicateCondition implements ConditionType<EntityCtx, PredicateCondition.Cfg> {
    public record Cfg(ResourceLocation predicate) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("predicate").forGetter(Cfg::predicate)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return false;
        HolderGetter.Provider getter = serverLevel.getServer().reloadableRegistries().lookup();
        ResourceKey<LootItemCondition> key = ResourceKey.create(Registries.PREDICATE, cfg.predicate);
        Holder.Reference<LootItemCondition> holder = getter.lookupOrThrow(Registries.PREDICATE)
            .get(key).orElse(null);
        if (holder == null) return false;
        LootItemCondition condition = holder.value();
        LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.THIS_ENTITY, ctx.raw())
            .withParameter(LootContextParams.ORIGIN, ctx.raw().position())
            .create(LootContextParamSets.SELECTOR);
        LootContext lootCtx = new LootContext.Builder(params).create(null);
        return condition.test(lootCtx);
    }
}
