package dev.overgrown.apoli.compat.appleskin;

import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.food.FoodValues;

public final class AppleSkinCompat implements AppleSkinApi {

    @Override
    public void registerEvents() {
        FoodValuesEvent.EVENT.register(event -> {
            FoodValues current = event.modifiedFoodValues;
            if (current == null) return;
            ModifyFoodHandler.Values modified = ModifyFoodHandler.values(event.player, event.itemStack,
                current.hunger, current.saturationModifier);
            if (modified != null) event.modifiedFoodValues = new FoodValues(modified.nutrition(), modified.saturation());
        });
    }
}
