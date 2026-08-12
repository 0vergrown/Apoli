package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record Ingredient(List<IdOrTag<Item>> entries) {

    private record Fields(Optional<ResourceLocation> item, Optional<ResourceLocation> tag) {}

    private static final Codec<Fields> FIELDS_CODEC = RecordCodecBuilder.create(i -> i.group(
        IdCodecs.ID.optionalFieldOf("item").forGetter(Fields::item),
        IdCodecs.TAG.optionalFieldOf("tag").forGetter(Fields::tag)
    ).apply(i, Fields::new));

    private static final Codec<IdOrTag<Item>> OBJECT_ENTRY = FIELDS_CODEC.comapFlatMap(
        Ingredient::fromFields,
        entry -> entry.isTag()
            ? new Fields(Optional.empty(), Optional.of(entry.id()))
            : new Fields(Optional.of(entry.id()), Optional.empty()));

    public static final Codec<IdOrTag<Item>> ENTRY_CODEC =
        Codec.either(IdOrTag.<Item>codec(Registries.ITEM), OBJECT_ENTRY)
            .xmap(either -> either.map(entry -> entry, entry -> entry), Either::left);

    public static final Codec<Ingredient> CODEC =
        Codec.either(ENTRY_CODEC, Codec.list(ENTRY_CODEC)).xmap(
            either -> either.map(e -> new Ingredient(List.of(e)), Ingredient::new),
            ing -> ing.entries.size() == 1 ? Either.left(ing.entries.get(0)) : Either.right(ing.entries));

    private static DataResult<IdOrTag<Item>> fromFields(Fields fields) {
        if (fields.tag().isPresent()) {
            return DataResult.success(IdOrTag.tag(Registries.ITEM, fields.tag().get()));
        }
        if (fields.item().isPresent()) {
            return DataResult.success(IdOrTag.id(fields.item().get()));
        }
        return DataResult.error(() -> "an ingredient needs either \"item\" or \"tag\"");
    }

    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).matches(stack.getItemHolder())) return true;
        }
        return false;
    }
}
