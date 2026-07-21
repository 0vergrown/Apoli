package dev.overgrown.apoli.condition.builtin.meta;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BiomeCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.FluidCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.TypedConditionRegistry;
import net.minecraft.resources.ResourceLocation;

public final class MetaConditions {
    private MetaConditions() {}

    private static final ResourceLocation ALL_OF = Apoli.id("all_of");
    private static final ResourceLocation ANY_OF = Apoli.id("any_of");
    private static final ResourceLocation RANDOM_CHANCE = Apoli.id("random_chance");
    private static final ResourceLocation CONSTANT = Apoli.id("constant");

    private static AliasingOptions allOfAliases() {
        return AliasingOptions.builder().addTypeAlias(Apoli.id("and")).build();
    }
    private static AliasingOptions anyOfAliases() {
        return AliasingOptions.builder().addTypeAlias(Apoli.id("or")).build();
    }
    private static AliasingOptions randomChanceAliases() {
        return AliasingOptions.builder().addTypeAlias(Apoli.id("chance")).build();
    }

    public static void registerEntity() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.EntityCtx> reg = ConditionTypes.ENTITY;
        reg.register(ALL_OF, new AllOfMeta<>(EntityCondition.CODEC, EntityCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(EntityCondition.CODEC, EntityCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
    }

    public static void registerBiEntity() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.BiEntityCtx> reg = ConditionTypes.BI_ENTITY;
        reg.register(ALL_OF, new AllOfMeta<>(BiEntityCondition.CODEC, BiEntityCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(BiEntityCondition.CODEC, BiEntityCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
    }

    public static void registerBlock() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.BlockCtx> reg = ConditionTypes.BLOCK;
        reg.register(ALL_OF, new AllOfMeta<>(BlockCondition.CODEC, BlockCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(BlockCondition.CODEC, BlockCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
        reg.register(Apoli.id("offset"), new OffsetBlockMetaCondition());
    }

    public static void registerItem() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.ItemCtx> reg = ConditionTypes.ITEM;
        reg.register(ALL_OF, new AllOfMeta<>(ItemCondition.CODEC, ItemCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(ItemCondition.CODEC, ItemCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
    }

    public static void registerDamage() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.DamageCtx> reg = ConditionTypes.DAMAGE;
        reg.register(ALL_OF, new AllOfMeta<>(DamageCondition.CODEC, DamageCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(DamageCondition.CODEC, DamageCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
    }

    public static void registerFluid() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.FluidCtx> reg = ConditionTypes.FLUID;
        reg.register(ALL_OF, new AllOfMeta<>(FluidCondition.CODEC, FluidCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(FluidCondition.CODEC, FluidCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
    }

    public static void registerBiome() {
        TypedConditionRegistry<dev.overgrown.apoli.condition.context.BiomeCtx> reg = ConditionTypes.BIOME;
        reg.register(ALL_OF, new AllOfMeta<>(BiomeCondition.CODEC, BiomeCondition::test), allOfAliases());
        reg.register(ANY_OF, new AnyOfMeta<>(BiomeCondition.CODEC, BiomeCondition::test), anyOfAliases());
        reg.register(RANDOM_CHANCE, new RandomChanceMeta<>(), randomChanceAliases());
        reg.register(CONSTANT, new ConstantMeta<>());
    }
}
