package dev.overgrown.apoli.compat.hardcorerevival;

import dev.overgrown.apoli.compat.hardcorerevival.power.ActionOnKnockoutPower;
import dev.overgrown.apoli.compat.hardcorerevival.power.ActionOnRevivePower;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.hardcorerevival.api.PlayerKnockedOutEvent;
import net.blay09.mods.hardcorerevival.api.PlayerRescuedEvent;
import net.blay09.mods.hardcorerevival.api.PlayerRevivedEvent;


public final class HardcoreRevivalCompat {
    private HardcoreRevivalCompat() {}

    public static void init() {
        Balm.getEvents().onEvent(PlayerKnockedOutEvent.class, event ->
            ActionOnKnockoutPower.handle(event.getPlayer()));
        Balm.getEvents().onEvent(PlayerRevivedEvent.class, event ->
            ActionOnRevivePower.handleRevived(event.getPlayer()));
        Balm.getEvents().onEvent(PlayerRescuedEvent.class, event ->
            ActionOnRevivePower.handleRescued(event.getPlayer(), event.getRescuer()));
    }
}
