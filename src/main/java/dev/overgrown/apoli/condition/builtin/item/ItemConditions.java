package dev.overgrown.apoli.condition.builtin.item;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class ItemConditions {
    private ItemConditions() {}

    public static void register() {
        ConditionTypes.ITEM.register(Apoli.id("amount"), new AmountItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("armor_value"), new ArmorValueItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("durability"), new DurabilityItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("empty"), new EmptyItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("enchantable"), new EnchantableItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("enchantment"), new EnchantmentCondition());
        ConditionTypes.ITEM.register(Apoli.id("fireproof"), new FireproofItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("food"), new FoodItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("fuel"), new FuelItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("harvest_level"), new HarvestLevelCondition());
        ConditionTypes.ITEM.register(Apoli.id("has_power"), new HasPowerItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("ingredient"), new IngredientCondition());
        ConditionTypes.ITEM.register(Apoli.id("is_damageable"), new IsDamageableItemCondition());
        ConditionTypes.ITEM.register(
            Apoli.id("is_equippable"),
            new IsEquippableItemCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("equippable")).build()
        );
        ConditionTypes.ITEM.register(Apoli.id("nbt"), new NbtItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("power_count"), new PowerCountItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("relative_durability"), new RelativeDurabilityItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("smeltable"), new SmeltableItemCondition());
    }
}
