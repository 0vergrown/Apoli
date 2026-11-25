package dev.overgrown.apoli.compat.appleskin;

import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.event.FoodValuesEvent;

public final class AppleSkinCompat implements AppleSkinApi {

    @Override
    public void registerEvents() {
        FoodValuesEvent.EVENT.register(event ->
            event.modifiedFoodComponent = ModifyFoodHandler.modify(event.player, event.itemStack, event.modifiedFoodComponent));
    }
}
