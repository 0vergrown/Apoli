package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecipePower extends PowerType<RecipePower.Config> {
    public record Config(Dynamic<?> recipe, @Nullable ResourceLocation recipeId) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.PASSTHROUGH.fieldOf("recipe").forGetter(Config::recipe)
        ).apply(i, recipe -> new Config(embedResultPowers(recipe), extractId(recipe))));
    }

    private static @Nullable ResourceLocation extractId(Dynamic<?> recipe) {
        return recipe.get("id").asString().result().map(ResourceLocation::tryParse).orElse(null);
    }

    static <T> Dynamic<T> embedResultPowers(Dynamic<T> recipe) {
        Dynamic<T> result = recipe.get("result").result().orElse(null);
        if (result == null || result.getMapValues().result().isEmpty()) return recipe;

        List<Dynamic<T>> entries = new ArrayList<>();
        result.get("power").result().ifPresent(value -> addEntries(entries, value));
        result.get("powers").asStreamOpt().result()
            .ifPresent(stream -> stream.forEach(value -> addEntries(entries, value)));
        if (entries.isEmpty()) return recipe;

        Dynamic<T> components = result.get("components").result()
            .filter(c -> c.getMapValues().result().isPresent())
            .orElseGet(result::emptyMap);
        Dynamic<T> customData = components.get("minecraft:custom_data").result()
            .filter(c -> c.getMapValues().result().isPresent())
            .orElseGet(result::emptyMap);

        List<Dynamic<T>> powers = new ArrayList<>();
        customData.get("Powers").asStreamOpt().result().ifPresent(stream -> stream.forEach(powers::add));
        powers.addAll(entries);

        customData = customData.set("Powers", result.createList(powers.stream()));
        components = components.set("minecraft:custom_data", customData);

        Dynamic<T> updated = result
            .set("components", components)
            .remove("power")
            .remove("powers");
        return recipe.set("result", updated);
    }

    private static <T> void addEntries(List<Dynamic<T>> out, Dynamic<T> value) {
        String simple = value.asString().result().orElse(null);
        if (simple != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                out.add(entry(value, simple, slot.getSerializedName(), false, false));
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
                out.add(entry(value, id, slot.getSerializedName(), hidden, negative));
            }
            return;
        }
        var stream = slots.asStreamOpt().result().orElse(null);
        if (stream != null) {
            stream.forEach(s -> s.asString().result()
                .ifPresent(name -> out.add(entry(value, id, name, hidden, negative))));
            return;
        }
        slots.asString().result().ifPresent(name -> out.add(entry(value, id, name, hidden, negative)));
    }

    private static <T> Dynamic<T> entry(Dynamic<T> ctx, String id, String slot, boolean hidden, boolean negative) {
        Dynamic<T> e = ctx.emptyMap()
            .set("Id", ctx.createString(id))
            .set("Slot", ctx.createString(slot));
        if (hidden) e = e.set("Hidden", ctx.createBoolean(true));
        if (negative) e = e.set("Negative", ctx.createBoolean(true));
        return e;
    }
}
