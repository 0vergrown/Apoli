package dev.overgrown.apoli.power.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import dev.overgrown.apoli.data.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecipePower extends PowerType<RecipePower.Config> {
    public record Config(Dynamic<?> recipe, @Nullable ResourceLocation recipeId, CompoundTag resultPowers) {

        /** 1.20.1's {@code RecipeManager.fromJson} predates recipe codecs and still demands a Gson tree. */
        public JsonObject recipeAsJson() {
            JsonElement element = recipe.convert(JsonOps.INSTANCE).getValue();
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.PASSTHROUGH.fieldOf("recipe").forGetter(Config::recipe)
        ).apply(i, RecipePower::build));
    }

    private static Config build(Dynamic<?> recipe) {
        return new Config(stripPowerFields(recipe), extractId(recipe), extractResultPowers(recipe));
    }

    /** Vanilla's 1.20.1 recipe parser rejects unknown result fields, so drop them once they are read. */
    private static <T> Dynamic<T> stripPowerFields(Dynamic<T> recipe) {
        Dynamic<T> result = recipe.get("result").result().orElse(null);
        if (result == null || result.getMapValues().result().isEmpty()) return recipe;
        return recipe.set("result", result.remove("power").remove("powers"));
    }

    private static @Nullable ResourceLocation extractId(Dynamic<?> recipe) {
        return recipe.get("id").asString().result().map(ResourceLocation::tryParse).orElse(null);
    }

    private static <T> CompoundTag extractResultPowers(Dynamic<T> recipe) {
        CompoundTag tag = new CompoundTag();
        Dynamic<T> result = recipe.get("result").result().orElse(null);
        if (result == null || result.getMapValues().result().isEmpty()) return tag;

        List<CompoundTag> entries = new ArrayList<>();
        result.get("power").result().ifPresent(value -> addEntries(entries, value));
        result.get("powers").asStreamOpt().result()
            .ifPresent(stream -> stream.forEach(value -> addEntries(entries, value)));
        if (entries.isEmpty()) return tag;

        ListTag powers = new ListTag();
        powers.addAll(entries);
        tag.put("Powers", powers);
        return tag;
    }

    private static <T> void addEntries(List<CompoundTag> out, Dynamic<T> value) {
        String simple = value.asString().result().orElse(null);
        if (simple != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                out.add(entry(simple, slot.getSerializedName(), false, false));
            }
            return;
        }
        String id = value.get("power").asString().result().orElse(null);
        if (id == null) return;
        boolean hidden = value.get("hidden").asBoolean(false);
        boolean negative = value.get("negative").asBoolean(false);

        Dynamic<T> slots = value.get("slot").result().orElse(null);
        if (slots == null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                out.add(entry(id, slot.getSerializedName(), hidden, negative));
            }
            return;
        }
        var stream = slots.asStreamOpt().result().orElse(null);
        if (stream != null) {
            stream.forEach(s -> s.asString().result()
                .ifPresent(name -> out.add(entry(id, name, hidden, negative))));
            return;
        }
        slots.asString().result().ifPresent(name -> out.add(entry(id, name, hidden, negative)));
    }

    private static CompoundTag entry(String id, String slot, boolean hidden, boolean negative) {
        CompoundTag c = new CompoundTag();
        c.putString("Id", id);
        c.putString("Slot", slot);
        if (hidden) c.putBoolean("Hidden", true);
        if (negative) c.putBoolean("Negative", true);
        return c;
    }
}
