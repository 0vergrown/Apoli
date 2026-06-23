package dev.overgrown.apoli.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record Ingredient(List<Entry> entries) {

    public static final Codec<Ingredient> CODEC = Codec.either(Entry.CODEC, Codec.list(Entry.CODEC)).xmap(
        either -> either.map(e -> new Ingredient(List.of(e)), Ingredient::new),
        ing -> ing.entries.size() == 1 ? Either.left(ing.entries.get(0)) : Either.right(ing.entries)
    );

    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (Entry e : entries) {
            if (e.test(stack)) return true;
        }
        return false;
    }

    public record Entry(Optional<ResourceLocation> item, Optional<ResourceLocation> tag) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(Entry::item),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(Entry::tag)
        ).apply(i, Entry::new));

        public boolean test(ItemStack stack) {
            if (item.isPresent()) {
                Item expected = BuiltInRegistries.ITEM.get(item.get());
                return stack.getItem() == expected;
            }
            if (tag.isPresent()) {
                TagKey<Item> key = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag.get());
                return stack.is(key);
            }
            return false;
        }
    }
}
