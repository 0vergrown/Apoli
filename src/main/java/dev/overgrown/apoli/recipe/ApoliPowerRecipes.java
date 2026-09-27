package dev.overgrown.apoli.recipe;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.RecipePower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ApoliPowerRecipes {
    private ApoliPowerRecipes() {}

    private static final Comparator<ResourceLocation> BY_ID =
        Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath);

    private static final Map<ResourceLocation, List<ResourceLocation>> RECIPE_TO_POWERS = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> DECLARED_IDS = new HashMap<>();
    private static final Map<ResourceLocation, CompoundTag> RECIPE_RESULT_POWERS = new HashMap<>();

    public static void inject(MinecraftServer server) {
        RECIPE_TO_POWERS.clear();
        DECLARED_IDS.clear();
        RECIPE_RESULT_POWERS.clear();

        Map<ResourceLocation, RecipePower.Config> claims = new HashMap<>();
        List<Recipe<?>> powerRecipes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, RecipePower.Config> e : recipePowers()) {
            ResourceLocation powerId = e.getKey();
            RecipePower.Config cfg = e.getValue();
            ResourceLocation declaredId = cfg.recipeId() != null ? cfg.recipeId() : powerId;
            ResourceLocation recipeId = claimable(claims, declaredId, cfg) ? declaredId : powerId;
            if (!claimable(claims, recipeId, cfg)) {
                Apoli.LOGGER.warn("[Apoli] apoli:recipe power {} was skipped: recipe ids {} and {} are both taken by different recipes.",
                    powerId, declaredId, recipeId);
                continue;
            }
            List<ResourceLocation> sharing = RECIPE_TO_POWERS.get(recipeId);
            if (sharing != null) {
                sharing.add(powerId);
                continue;
            }

            Recipe<?> recipe;
            try {
                recipe = RecipeManager.fromJson(recipeId, cfg.recipeAsJson());
            } catch (Exception ex) {
                Apoli.LOGGER.warn("[Apoli] apoli:recipe power {} has an invalid recipe: {}", powerId, ex.getMessage());
                continue;
            }
            if (recipe == null) continue;

            powerRecipes.add(recipe);
            claims.put(recipeId, cfg);
            List<ResourceLocation> powers = new ArrayList<>(1);
            powers.add(powerId);
            RECIPE_TO_POWERS.put(recipeId, powers);
            if (!recipeId.equals(declaredId)) {
                DECLARED_IDS.put(recipeId, declaredId);
                Apoli.LOGGER.warn("[Apoli] apoli:recipe power {} reuses recipe id {} from {} for a different recipe; registered it as {}.",
                    powerId, declaredId, RECIPE_TO_POWERS.get(declaredId).get(0), recipeId);
            }
            if (cfg.resultPowers().contains("Powers")) {
                RECIPE_RESULT_POWERS.put(recipeId, cfg.resultPowers());
            }
        }

        RecipeManager rm = server.getRecipeManager();
        List<Recipe<?>> all = new ArrayList<>();
        for (Recipe<?> existing : rm.getRecipes()) {
            if (!claims.containsKey(existing.getId())) all.add(existing);
        }
        all.addAll(powerRecipes);
        rm.replaceRecipes(all);

        if (!powerRecipes.isEmpty()) {
            Apoli.LOGGER.info("[Apoli] Registered {} power-gated recipe(s).", powerRecipes.size());
        }
    }

    private static List<Map.Entry<ResourceLocation, RecipePower.Config>> recipePowers() {
        List<Map.Entry<ResourceLocation, RecipePower.Config>> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Power> e : ApoliPowers.view().entrySet()) {
            Power power = e.getValue();
            if (PowerTypeRegistry.get(power.typeId()) instanceof RecipePower
                && power.config() instanceof RecipePower.Config cfg) {
                out.add(Map.entry(e.getKey(), cfg));
            }
        }
        out.sort(Map.Entry.comparingByKey(BY_ID));
        return out;
    }

    private static boolean claimable(Map<ResourceLocation, RecipePower.Config> claims, ResourceLocation recipeId,
                                     RecipePower.Config cfg) {
        RecipePower.Config claimed = claims.get(recipeId);
        return claimed == null || claimed.equals(cfg);
    }

    public static boolean isPowerRecipe(ResourceLocation recipeId) {
        return RECIPE_TO_POWERS.containsKey(recipeId);
    }

    public static boolean canCraft(Player player, ResourceLocation recipeId) {
        List<ResourceLocation> powers = RECIPE_TO_POWERS.get(recipeId);
        if (powers == null) return true;
        PowerContainer container = PowerContainer.of(player);
        if (container == null) return false;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.hasPower(powerId) && !container.isSuppressed(powerId)) return true;
        }
        return false;
    }

    public static <C extends Container, T extends Recipe<C>> Optional<T> firstCraftable(RecipeManager manager, RecipeType<T> type,
                                                                                        C container, Level level, Player player) {
        List<T> matches = manager.getRecipesFor(type, container, level);
        for (int i = 0; i < matches.size(); i++) {
            T recipe = matches.get(i);
            if (canCraft(player, recipe.getId())) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    public static @Nullable ResourceLocation declaredId(@Nullable ResourceLocation recipeId) {
        if (recipeId == null) return null;
        ResourceLocation declared = DECLARED_IDS.get(recipeId);
        return declared != null ? declared : recipeId;
    }

    public static @Nullable CompoundTag resultPowersFor(ResourceLocation recipeId) {
        return RECIPE_RESULT_POWERS.get(recipeId);
    }
}
