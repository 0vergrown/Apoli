package dev.overgrown.apoli.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class LoaderIngredients {
    private LoaderIngredients() {}

    static @Nullable Ingredient.CustomType<?> find(boolean fabric, ResourceLocation id) {
        if (fabric) return null;
        IngredientType<?> type = NeoForgeRegistries.INGREDIENT_TYPES.get(id);
        return type == null ? null : wrap(id, type);
    }

    @SuppressWarnings("unchecked")
    private static <I extends ICustomIngredient> Ingredient.CustomType<Delegate> wrap(ResourceLocation id,
                                                                                      IngredientType<I> ingredientType) {
        return new Ingredient.CustomType<>(Ingredient.TYPE_KEY, id, type -> ingredientType.codec().xmap(
            ingredient -> new Delegate(type, ingredient),
            delegate -> (I) delegate.ingredient()));
    }

    public record Delegate(Ingredient.CustomType<Delegate> type, ICustomIngredient ingredient) implements Ingredient.Custom {
        @Override
        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }
    }
}
