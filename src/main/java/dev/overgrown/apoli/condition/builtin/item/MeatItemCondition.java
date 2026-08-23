package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public final class MeatItemCondition implements ConditionType<ItemCtx, EmptyCfg> {
    private static final TagKey<Item> CONVENTION_MEAT =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/meat"));

    private static final Set<Item> VANILLA_MEAT = Set.of(
        Items.BEEF, Items.COOKED_BEEF,
        Items.PORKCHOP, Items.COOKED_PORKCHOP,
        Items.MUTTON, Items.COOKED_MUTTON,
        Items.CHICKEN, Items.COOKED_CHICKEN,
        Items.RABBIT, Items.COOKED_RABBIT,
        Items.COD, Items.COOKED_COD,
        Items.SALMON, Items.COOKED_SALMON,
        Items.TROPICAL_FISH, Items.PUFFERFISH,
        Items.ROTTEN_FLESH, Items.SPIDER_EYE
    );

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, ItemCtx ctx) {
        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty()) return false;
        FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        if (food == null) return false;
        return stack.is(CONVENTION_MEAT) || VANILLA_MEAT.contains(stack.getItem());
    }
}
