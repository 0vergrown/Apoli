package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public record PositionedItemStack(ItemStack stack, OptionalInt slot) {

    public static final Codec<PositionedItemStack> CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<PositionedItemStack>mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(s -> s.stack.getItem()),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(s -> s.stack.getCount()),
            Nbt.CODEC.optionalFieldOf("tag").forGetter(s -> s.stack.hasTag()
                ? Optional.of(new Nbt(s.stack.getTag()))
                : Optional.empty()),
            Codec.INT.optionalFieldOf("slot").forGetter(s -> s.slot.isPresent()
                ? Optional.of(s.slot.getAsInt())
                : Optional.empty())
        ).apply(i, PositionedItemStack::build)),
        Map.of("id", "item", "count", "amount")
    ).codec();

    private static PositionedItemStack build(Item item, Integer amount, Optional<Nbt> tag, Optional<Integer> slot) {
        ItemStack stack = new ItemStack(item, amount);
        tag.ifPresent(n -> stack.setTag(n.tag()));
        return new PositionedItemStack(stack, slot.map(OptionalInt::of).orElse(OptionalInt.empty()));
    }
}
