package dev.overgrown.apoli.compat.accessory;

import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.compat.accessory.backend.AccessoriesBackend;
import dev.overgrown.apoli.compat.accessory.backend.TrinketsBackend;
import dev.overgrown.apoli.compat.accessory.power.ActionOnAccessoryChangePower;

public final class AccessoryCompat {
    private AccessoryCompat() {}

    public static void init() {
        if (ModCompat.TRINKETS) {
            Accessories.register(new TrinketsBackend());
        }
        if (ModCompat.ACCESSORIES) {
            AccessoriesBackend accessories = new AccessoriesBackend();
            Accessories.register(accessories);
            accessories.registerEvents();
        }
        if (Accessories.anyPresent()) {
            Accessories.setChangeListener(ActionOnAccessoryChangePower::handle);
        }
    }
}
