package dev.overgrown.apoli.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

public final class LoaderIngredients {
    private LoaderIngredients() {}

    static @Nullable Ingredient.CustomType<?> find(boolean fabric, ResourceLocation id) {
        if (!fabric) return null;
        CustomIngredientSerializer<?> serializer = CustomIngredientSerializer.get(id);
        return serializer == null ? null : wrap(id, serializer);
    }

    private static <I extends CustomIngredient> Ingredient.CustomType<Delegate> wrap(ResourceLocation id,
                                                                                     CustomIngredientSerializer<I> serializer) {
        return new Ingredient.CustomType<>(Ingredient.FABRIC_KEY, id, type -> new MapCodec<Delegate>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.empty();
            }

            @Override
            public <T> DataResult<Delegate> decode(DynamicOps<T> ops, MapLike<T> input) {
                JsonElement json = new Dynamic<>(ops, ops.createMap(input.entries())).convert(JsonOps.INSTANCE).getValue();
                if (!(json instanceof JsonObject object)) {
                    return DataResult.error(() -> "Custom ingredient '" + id + "' must be an object");
                }
                try {
                    return DataResult.success(new Delegate(type, serializer.read(object)));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Custom ingredient '" + id + "' failed to load: " + e.getMessage());
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> RecordBuilder<T> encode(Delegate input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                JsonObject json = new JsonObject();
                serializer.write(json, (I) input.ingredient());
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    if (Ingredient.FABRIC_KEY.equals(entry.getKey())) continue;
                    prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
                }
                return prefix;
            }
        });
    }

    public record Delegate(Ingredient.CustomType<Delegate> type, CustomIngredient ingredient) implements Ingredient.Custom {
        @Override
        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }
    }
}
