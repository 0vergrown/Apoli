package dev.overgrown.apoli.item;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public final class ApoliLootFunctions {
    private ApoliLootFunctions() {}

    public static LootItemFunctionType ADD_POWER;
    public static LootItemFunctionType REMOVE_POWER;

    public static void register() {
        ADD_POWER = Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Apoli.id("add_power"),
            new LootItemFunctionType(new AddPowerFunction.Serializer()));
        REMOVE_POWER = Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Apoli.id("remove_power"),
            new LootItemFunctionType(new RemovePowerFunction.Serializer()));
    }
}
