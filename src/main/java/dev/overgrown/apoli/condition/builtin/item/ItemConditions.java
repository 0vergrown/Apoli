package dev.overgrown.apoli.condition.builtin.item;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.compat.accessory.condition.item.AccessoryCondition;
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
        ConditionTypes.ITEM.register(
            Apoli.id("fireproof"),
            new FireproofItemCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("fire_resistant")).build()
        );
        ConditionTypes.ITEM.register(Apoli.id("food"), new FoodItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("fuel"), new FuelItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("harvest_level"), new HarvestLevelCondition());
        ConditionTypes.ITEM.register(Apoli.id("has_power"), new HasPowerItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("ingredient"), new IngredientCondition());
        ConditionTypes.ITEM.register(
            Apoli.id("is_damageable"),
            new IsDamageableItemCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("damageable")).build()
        );
        ConditionTypes.ITEM.register(
            Apoli.id("is_equippable"),
            new IsEquippableItemCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("equippable")).build()
        );
        ConditionTypes.ITEM.register(
            Apoli.id("nbt"),
            new NbtItemCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("custom_data")).build()
        );
        ConditionTypes.ITEM.register(Apoli.id("meat"), new MeatItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("relative_item_cooldown"), new RelativeItemCooldownCondition());
        ConditionTypes.ITEM.register(Apoli.id("power_count"), new PowerCountItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("relative_durability"), new RelativeDurabilityItemCondition());
        ConditionTypes.ITEM.register(Apoli.id("smeltable"), new SmeltableItemCondition());

        if (ModCompat.anyAccessory()) {
            ConditionTypes.ITEM.register(Apoli.id("accessory"), new AccessoryCondition(),
                AliasingOptions.builder().addTypeAlias(Apoli.id("trinket")).build());
        }

        ConditionTypes.ITEM.register(Apoli.id("script"), new ScriptItemCondition());

        ConditionTypes.ITEM.register(Apoli.id("is_block"), new IsBlockItemCondition(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("block"))
                .addTypeAlias("shappoli:is_block")
                .addTypeAlias("shappoli:block")
                .build());
        ConditionTypes.ITEM.register(Apoli.id("holder_condition"), new HolderItemCondition(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("holder"))
                .addTypeAlias("shappoli:holder_condition")
                .addTypeAlias("shappoli:holder")
                .build());
        ConditionTypes.ITEM.register(Apoli.id("send_condition"),
            new dev.overgrown.apoli.condition.builtin.meta.SendConditionMeta.Item(),
            AliasingOptions.builder().addTypeAlias("shappoli:send_condition").build());
    }
}
