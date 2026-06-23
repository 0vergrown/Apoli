package dev.overgrown.apoli.power.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RecipePower extends PowerType<RecipePower.Config> {
    public record Config(JsonObject recipe, ResourceLocation recipeId) {}

    private static final Codec<JsonObject> JSON_OBJECT_CODEC = Codec.PASSTHROUGH.xmap(
        dyn -> {
            JsonElement el = dyn.convert(JsonOps.INSTANCE).getValue();
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        },
        obj -> new com.mojang.serialization.Dynamic<>(JsonOps.INSTANCE, obj)
    );

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            JSON_OBJECT_CODEC.fieldOf("recipe").forGetter(Config::recipe)
        ).apply(i, json -> new Config(embedResultPowers(json), extractId(json))));
    }

    private static @Nullable ResourceLocation extractId(JsonObject recipe) {
        if (recipe == null || !recipe.has("id")) return null;
        try {
            return ResourceLocation.parse(recipe.get("id").getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    static JsonObject embedResultPowers(JsonObject recipe) {
        if (recipe == null || !recipe.has("result") || !recipe.get("result").isJsonObject()) return recipe;
        JsonObject result = recipe.getAsJsonObject("result");

        JsonArray entries = new JsonArray();
        if (result.has("power")) addEntries(entries, result.get("power"));
        if (result.has("powers") && result.get("powers").isJsonArray()) {
            for (JsonElement el : result.getAsJsonArray("powers")) addEntries(entries, el);
        }
        if (entries.isEmpty()) return recipe;

        JsonObject components = result.has("components") && result.get("components").isJsonObject()
            ? result.getAsJsonObject("components") : new JsonObject();
        JsonObject customData = components.has("minecraft:custom_data") && components.get("minecraft:custom_data").isJsonObject()
            ? components.getAsJsonObject("minecraft:custom_data") : new JsonObject();
        JsonArray powers = customData.has("Powers") && customData.get("Powers").isJsonArray()
            ? customData.getAsJsonArray("Powers") : new JsonArray();
        powers.addAll(entries);
        customData.add("Powers", powers);
        components.add("minecraft:custom_data", customData);
        result.add("components", components);
        result.remove("power");
        result.remove("powers");
        return recipe;
    }

    private static void addEntries(JsonArray out, JsonElement value) {
        if (value.isJsonPrimitive()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                out.add(entry(value.getAsString(), slot.getSerializedName(), false, false));
            }
            return;
        }
        if (!value.isJsonObject()) return;
        JsonObject o = value.getAsJsonObject();
        if (!o.has("power")) return;
        String id = o.get("power").getAsString();
        boolean hidden = o.has("hidden") && o.get("hidden").getAsBoolean();
        boolean negative = o.has("negative") && o.get("negative").getAsBoolean();
        if (o.has("slot")) {
            JsonElement sl = o.get("slot");
            if (sl.isJsonArray()) {
                for (JsonElement s : sl.getAsJsonArray()) out.add(entry(id, s.getAsString(), hidden, negative));
            } else {
                out.add(entry(id, sl.getAsString(), hidden, negative));
            }
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                out.add(entry(id, slot.getSerializedName(), hidden, negative));
            }
        }
    }

    private static JsonObject entry(String id, String slot, boolean hidden, boolean negative) {
        JsonObject e = new JsonObject();
        e.addProperty("Id", id);
        e.addProperty("Slot", slot);
        if (hidden) e.addProperty("Hidden", true);
        if (negative) e.addProperty("Negative", true);
        return e;
    }
}
