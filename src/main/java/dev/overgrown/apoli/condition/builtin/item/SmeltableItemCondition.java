package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

public final class SmeltableItemCondition implements ConditionType<ItemCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, ItemCtx ctx) {
        if (ctx.stack() == null || ctx.stack().isEmpty()) return false;
        if (ctx.level() == null) return false;
        return ctx.level().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(ctx.stack()), ctx.level())
            .isPresent();
    }
}
