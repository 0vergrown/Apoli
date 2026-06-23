package dev.overgrown.apoli.item;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public final class ApoliLootFunctions {
    private ApoliLootFunctions() {}

    public static LootItemFunctionType<AddPowerFunction> ADD_POWER;
    public static LootItemFunctionType<RemovePowerFunction> REMOVE_POWER;

    public static void register() {
        ADD_POWER = Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Apoli.id("add_power"),
            new LootItemFunctionType<>(AddPowerFunction.CODEC));
        REMOVE_POWER = Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Apoli.id("remove_power"),
            new LootItemFunctionType<>(RemovePowerFunction.CODEC));
    }
}
