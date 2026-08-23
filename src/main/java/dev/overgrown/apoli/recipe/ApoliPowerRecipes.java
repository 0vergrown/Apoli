package dev.overgrown.apoli.recipe;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.RecipePower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApoliPowerRecipes {
    private ApoliPowerRecipes() {}

    private static final Map<ResourceLocation, ResourceLocation> RECIPE_TO_POWER = new HashMap<>();
    private static final Map<ResourceLocation, CompoundTag> RECIPE_RESULT_POWERS = new HashMap<>();

    public static void inject(MinecraftServer server) {
        RECIPE_TO_POWER.clear();
        RECIPE_RESULT_POWERS.clear();

        List<Recipe<?>> powerRecipes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            Power power = e.getValue();
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof RecipePower)) continue;
            if (!(power.config() instanceof RecipePower.Config cfg)) continue;

            ResourceLocation recipeId = cfg.recipeId() != null ? cfg.recipeId() : e.getKey();
            Recipe<?> recipe;
            try {
                recipe = RecipeManager.fromJson(recipeId, cfg.recipeAsJson());
            } catch (Exception ex) {
                Apoli.LOGGER.warn("[Apoli] apoli:recipe power {} has an invalid recipe: {}", e.getKey(), ex.getMessage());
                continue;
            }
            if (recipe == null) continue;

            powerRecipes.add(recipe);
            RECIPE_TO_POWER.put(recipeId, e.getKey());
            if (cfg.resultPowers().contains("Powers")) {
                RECIPE_RESULT_POWERS.put(recipeId, cfg.resultPowers());
            }
        }

        RecipeManager rm = server.getRecipeManager();
        Set<ResourceLocation> ours = new HashSet<>();
        for (Recipe<?> r : powerRecipes) ours.add(r.getId());

        List<Recipe<?>> all = new ArrayList<>();
        for (Recipe<?> existing : rm.getRecipes()) {
            if (!ours.contains(existing.getId())) all.add(existing);
        }
        all.addAll(powerRecipes);
        rm.replaceRecipes(all);

        if (!powerRecipes.isEmpty()) {
            Apoli.LOGGER.info("[Apoli] Registered {} power-gated recipe(s).", powerRecipes.size());
        }
    }

    public static @Nullable ResourceLocation powerFor(ResourceLocation recipeId) {
        return RECIPE_TO_POWER.get(recipeId);
    }

    public static @Nullable CompoundTag resultPowersFor(ResourceLocation recipeId) {
        return RECIPE_RESULT_POWERS.get(recipeId);
    }
}
