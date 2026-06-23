package dev.overgrown.apoli.recipe;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.RecipePower;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
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

    public static void inject(MinecraftServer server) {
        RECIPE_TO_POWER.clear();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());

        List<RecipeHolder<?>> powerRecipes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            Power power = e.getValue();
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof RecipePower)) continue;
            if (!(power.config() instanceof RecipePower.Config cfg)) continue;

            ResourceLocation recipeId = cfg.recipeId() != null ? cfg.recipeId() : e.getKey();
            Recipe<?> recipe = Recipe.CODEC.parse(ops, cfg.recipe())
                .resultOrPartial(err -> Apoli.LOGGER.warn("[Apoli] apoli:recipe power {} has an invalid recipe: {}", e.getKey(), err))
                .orElse(null);
            if (recipe == null) continue;

            powerRecipes.add(new RecipeHolder<>(recipeId, recipe));
            RECIPE_TO_POWER.put(recipeId, e.getKey());
        }

        RecipeManager rm = server.getRecipeManager();
        Set<ResourceLocation> ours = new HashSet<>();
        for (RecipeHolder<?> h : powerRecipes) ours.add(h.id());

        List<RecipeHolder<?>> all = new ArrayList<>();
        for (RecipeHolder<?> existing : rm.getRecipes()) {
            if (!ours.contains(existing.id())) all.add(existing);
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
}
