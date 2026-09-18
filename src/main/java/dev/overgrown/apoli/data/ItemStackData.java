package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public record ItemStackData(ItemStack stack) {

    private static final int HIDE_UNBREAKABLE = 4;

    public static final MapCodec<ItemStackData> MAP_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.<ItemStackData>mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(s -> s.stack.getItem()),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(s -> s.stack.getCount()),
            LoggedOptionalField.of("tag", Nbt.CODEC).forGetter(s -> s.stack.hasTag()
                ? Optional.of(new Nbt(s.stack.getTag()))
                : Optional.empty()),
            TextComponent.CODEC.optionalFieldOf("name").forGetter(s -> Optional.empty()),
            Codec.BOOL.optionalFieldOf("unbreakable", false).forGetter(s -> false)
        ).apply(i, ItemStackData::build)),
        Map.of("id", "item", "count", "amount")
    );

    public static final Codec<ItemStackData> CODEC = MAP_CODEC.codec();

    private static ItemStackData build(Item item, Integer amount, Optional<Nbt> tag,
                                       Optional<Component> name, Boolean unbreakable) {
        ItemStack stack = new ItemStack(item, amount);
        tag.ifPresent(n -> stack.setTag(n.tag()));
        if (Boolean.TRUE.equals(unbreakable)) {
            CompoundTag root = stack.getOrCreateTag();
            root.putBoolean("Unbreakable", true);
            root.putInt("HideFlags", root.getInt("HideFlags") | HIDE_UNBREAKABLE);
        }
        name.ifPresent(component -> stack.setHoverName(ItemNames.upright(component)));
        return new ItemStackData(stack);
    }
}
