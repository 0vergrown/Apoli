package dev.overgrown.apoli.compat.appleskin;

import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.neoforged.neoforge.common.NeoForge;
import squeek.appleskin.api.event.FoodValuesEvent;

public final class AppleSkinCompat {
    private AppleSkinCompat() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(FoodValuesEvent.class, event ->
            event.modifiedFoodProperties = ModifyFoodHandler.modify(event.player, event.itemStack, event.modifiedFoodProperties));
    }
}
