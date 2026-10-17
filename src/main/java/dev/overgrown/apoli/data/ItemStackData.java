package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;

import java.util.Map;
import java.util.Optional;

public record ItemStackData(ItemStack stack) {

    public static final MapCodec<ItemStackData> MAP_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<ItemStackData>mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(s -> s.stack.getItem()),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(s -> s.stack.getCount()),
            LoggedOptionalField.of("components", DataComponentPatch.CODEC, DataComponentPatch.EMPTY).forGetter(s -> s.stack.getComponentsPatch()),
            TextComponent.CODEC.optionalFieldOf("name").forGetter(s -> Optional.empty()),
            Codec.BOOL.optionalFieldOf("unbreakable", false).forGetter(s -> false)
        ).apply(i, ItemStackData::build)),
        Map.of("id", "item", "count", "amount")
    );

    public static final Codec<ItemStackData> CODEC = MAP_CODEC.codec();

    public static boolean hasData(ItemStack template) {
        return !template.getComponentsPatch().isEmpty();
    }

    public static boolean sameData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    private static ItemStackData build(Item item, Integer amount, DataComponentPatch components,
                                       Optional<Component> name, Boolean unbreakable) {
        ItemStack stack = new ItemStack(item, amount);
        stack.applyComponents(components);
        if (Boolean.TRUE.equals(unbreakable)) {
            stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        }
        name.ifPresent(component -> stack.set(DataComponents.CUSTOM_NAME, ItemNames.upright(component)));
        return new ItemStackData(stack);
    }
}
