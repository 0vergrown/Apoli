package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public record ItemStackData(ItemStack stack) {

    public static final Codec<ItemStackData> CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<ItemStackData>mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(s -> s.stack.getItem()),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(s -> s.stack.getCount()),
            LoggedOptionalField.of("tag", Nbt.CODEC).forGetter(s -> s.stack.hasTag()
                ? Optional.of(new Nbt(s.stack.getTag()))
                : Optional.empty())
        ).apply(i, ItemStackData::build)),
        Map.of("id", "item", "count", "amount")
    ).codec();

    private static ItemStackData build(Item item, Integer amount, Optional<Nbt> tag) {
        ItemStack stack = new ItemStack(item, amount);
        tag.ifPresent(n -> stack.setTag(n.tag()));
        return new ItemStackData(stack);
    }
}
