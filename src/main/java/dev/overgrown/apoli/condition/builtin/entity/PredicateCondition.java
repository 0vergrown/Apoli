package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootContext;

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
        LootItemCondition condition = serverLevel.getServer().getLootData()
            .getElement(LootDataType.PREDICATE, cfg.predicate);
        if (condition == null) return false;
        LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.THIS_ENTITY, ctx.raw())
            .withParameter(LootContextParams.ORIGIN, ctx.raw().position())
            .create(LootContextParamSets.SELECTOR);
        LootContext lootCtx = new LootContext.Builder(params).create(null);
        return condition.test(lootCtx);
    }

    @SuppressWarnings("unused")
    private static void keepImports(EntityPredicate p, ServerPlayer sp) {}

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
