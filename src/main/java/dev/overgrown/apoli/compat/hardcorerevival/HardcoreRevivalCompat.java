package dev.overgrown.apoli.compat.hardcorerevival;

import dev.overgrown.apoli.compat.hardcorerevival.power.ActionOnKnockoutPower;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.hardcorerevival.api.PlayerKnockedOutEvent;


public final class HardcoreRevivalCompat {
    private HardcoreRevivalCompat() {}

    public static void init() {
        Balm.getEvents().onEvent(PlayerKnockedOutEvent.class, event ->
            ActionOnKnockoutPower.handle(event.getPlayer()));
    }
}
