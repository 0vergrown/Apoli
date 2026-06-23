package dev.overgrown.apoli.condition;

import dev.overgrown.apoli.condition.builtin.bientity.BiEntityConditions;
import dev.overgrown.apoli.condition.builtin.biome.BiomeConditions;
import dev.overgrown.apoli.condition.builtin.block.BlockConditions;
import dev.overgrown.apoli.condition.builtin.damage.DamageConditions;
import dev.overgrown.apoli.condition.builtin.entity.EntityConditions;
import dev.overgrown.apoli.condition.builtin.fluid.FluidConditions;
import dev.overgrown.apoli.condition.builtin.item.ItemConditions;
import dev.overgrown.apoli.condition.builtin.meta.MetaConditions;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BiomeCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.FluidCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;

public final class ConditionTypes {
    public static final TypedConditionRegistry<EntityCtx> ENTITY = new TypedConditionRegistry<>("entity");
    public static final TypedConditionRegistry<BiEntityCtx> BI_ENTITY = new TypedConditionRegistry<>("bi_entity");
    public static final TypedConditionRegistry<BlockCtx> BLOCK = new TypedConditionRegistry<>("block");
    public static final TypedConditionRegistry<ItemCtx> ITEM = new TypedConditionRegistry<>("item");
    public static final TypedConditionRegistry<DamageCtx> DAMAGE = new TypedConditionRegistry<>("damage");
    public static final TypedConditionRegistry<FluidCtx> FLUID = new TypedConditionRegistry<>("fluid");
    public static final TypedConditionRegistry<BiomeCtx> BIOME = new TypedConditionRegistry<>("biome");

    private ConditionTypes() {}

    public static void bootstrap() {
        EntityConditions.register();
        BiEntityConditions.register();
        BlockConditions.register();
        FluidConditions.register();
        ItemConditions.register();
        DamageConditions.register();
        BiomeConditions.register();
        MetaConditions.registerEntity();
        MetaConditions.registerBiEntity();
        MetaConditions.registerBlock();
        MetaConditions.registerItem();
        MetaConditions.registerDamage();
        MetaConditions.registerFluid();
        MetaConditions.registerBiome();
    }
}
