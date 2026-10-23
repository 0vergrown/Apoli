package dev.overgrown.apoli.data;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class LoaderIngredients {
    private LoaderIngredients() {}

    static @Nullable Ingredient.CustomType<?> find(boolean fabric, ResourceLocation id) {
        if (!fabric) return null;
        CustomIngredientSerializer<?> serializer = CustomIngredientSerializer.get(id);
        return serializer == null ? null : wrap(id, serializer);
    }

    @SuppressWarnings("unchecked")
    private static <I extends CustomIngredient> Ingredient.CustomType<Delegate> wrap(ResourceLocation id,
                                                                                     CustomIngredientSerializer<I> serializer) {
        return new Ingredient.CustomType<>(Ingredient.FABRIC_KEY, id, type -> serializer.getCodec(true).xmap(
            ingredient -> new Delegate(type, ingredient),
            delegate -> (I) delegate.ingredient()));
    }

    public record Delegate(Ingredient.CustomType<Delegate> type, CustomIngredient ingredient) implements Ingredient.Custom {
        @Override
        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }
    }
}
