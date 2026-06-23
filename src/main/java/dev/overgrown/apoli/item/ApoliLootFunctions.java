package dev.overgrown.apoli.item;

import dev.overgrown.apoli.Apoli;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class ApoliLootFunctions {
    private ApoliLootFunctions() {}

    public static LootItemFunctionType<AddPowerFunction> ADD_POWER;
    public static LootItemFunctionType<RemovePowerFunction> REMOVE_POWER;

    public static void register(RegisterEvent event) {
        event.register(Registries.LOOT_FUNCTION_TYPE, helper -> {
            ADD_POWER = new LootItemFunctionType<>(AddPowerFunction.CODEC);
            REMOVE_POWER = new LootItemFunctionType<>(RemovePowerFunction.CODEC);
            helper.register(Apoli.id("add_power"), ADD_POWER);
            helper.register(Apoli.id("remove_power"), REMOVE_POWER);
        });
    }
}
