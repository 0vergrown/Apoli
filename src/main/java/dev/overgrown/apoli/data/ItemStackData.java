package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public record ItemStackData(ItemStack stack) {

    public static final Codec<ItemStackData> CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<ItemStackData>mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(s -> s.stack.getItem()),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(s -> s.stack.getCount()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(s -> s.stack.getComponentsPatch())
        ).apply(i, ItemStackData::build)),
        Map.of("id", "item", "count", "amount")
    ).codec();

    private static ItemStackData build(Item item, Integer amount, DataComponentPatch components) {
        ItemStack stack = new ItemStack(item, amount);
        stack.applyComponents(components);
        return new ItemStackData(stack);
    }
}
