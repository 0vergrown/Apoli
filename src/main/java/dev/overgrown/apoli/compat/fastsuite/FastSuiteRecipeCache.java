package dev.overgrown.apoli.compat.fastsuite;

import dev.overgrown.apoli.compat.fastsuite.mixin.AuxRecipeManagerAccessor;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Map;

public final class FastSuiteRecipeCache {
    private FastSuiteRecipeCache() {}

    public static void invalidate(RecipeManager manager) {
        if (!(manager instanceof AuxRecipeManagerAccessor aux)) return;
        Map<?, ?> lists = aux.apoli$cachedRecipeLists();
        synchronized (lists) {
            lists.clear();
        }
    }
}
