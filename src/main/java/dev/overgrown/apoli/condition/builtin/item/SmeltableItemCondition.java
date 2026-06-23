package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.RecipeType;

public final class SmeltableItemCondition implements ConditionType<ItemCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, ItemCtx ctx) {
        if (ctx.stack() == null || ctx.stack().isEmpty()) return false;
        if (ctx.level() == null) return false;
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, ctx.stack());
        return ctx.level().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, container, ctx.level())
            .isPresent();
    }
}
