package dev.overgrown.apoli.compat.accessory;

import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.compat.accessory.backend.AccessoriesBackend;
import dev.overgrown.apoli.compat.accessory.backend.CuriosBackend;
import dev.overgrown.apoli.compat.accessory.power.ActionOnAccessoryChangePower;
import net.neoforged.neoforge.common.NeoForge;

public final class AccessoryCompat {
    private AccessoryCompat() {}

    public static void init() {
        if (ModCompat.ACCESSORIES) {
            AccessoriesBackend accessories = new AccessoriesBackend();
            Accessories.register(accessories);
            accessories.registerEvents();
        }

        if (ModCompat.CURIOS) {
            Accessories.register(new CuriosBackend());
            NeoForge.EVENT_BUS.addListener(CuriosBackend::onCurioChange);
            NeoForge.EVENT_BUS.addListener(CuriosBackend::onCanEquip);
            NeoForge.EVENT_BUS.addListener(CuriosBackend::onCanUnequip);
        }

        if (Accessories.anyPresent()) {
            Accessories.setChangeListener(ActionOnAccessoryChangePower::handle);
        }
    }
}
