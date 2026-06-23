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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class RecipePower extends PowerType<RecipePower.Config> {
    public record Config(JsonObject recipe, ResourceLocation recipeId, CompoundTag resultPowers) {}

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
        ).apply(i, RecipePower::build));
    }

    private static Config build(JsonObject recipe) {
        return new Config(recipe, extractId(recipe), extractResultPowers(recipe));
    }

    private static @Nullable ResourceLocation extractId(JsonObject recipe) {
        if (recipe == null || !recipe.has("id")) return null;
        try {
            return ResourceLocation.tryParse(recipe.get("id").getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static CompoundTag extractResultPowers(JsonObject recipe) {
        CompoundTag tag = new CompoundTag();
        if (recipe == null || !recipe.has("result") || !recipe.get("result").isJsonObject()) return tag;
        JsonObject result = recipe.getAsJsonObject("result");

        JsonArray entries = new JsonArray();
        if (result.has("power")) addEntries(entries, result.get("power"));
        if (result.has("powers") && result.get("powers").isJsonArray()) {
            for (JsonElement el : result.getAsJsonArray("powers")) addEntries(entries, el);
        }
        result.remove("power");
        result.remove("powers");
        if (entries.isEmpty()) return tag;

        ListTag powers = new ListTag();
        for (JsonElement el : entries) powers.add(toCompound(el.getAsJsonObject()));
        tag.put("Powers", powers);
        return tag;
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

    private static CompoundTag toCompound(JsonObject o) {
        CompoundTag c = new CompoundTag();
        c.putString("Id", o.get("Id").getAsString());
        c.putString("Slot", o.get("Slot").getAsString());
        if (o.has("Hidden")) c.putBoolean("Hidden", true);
        if (o.has("Negative")) c.putBoolean("Negative", true);
        return c;
    }
}
