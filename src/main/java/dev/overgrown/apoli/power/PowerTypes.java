package dev.overgrown.apoli.power;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.ActionOverTimePower;
import dev.overgrown.apoli.power.builtin.CreativeFlightPower;

public final class PowerTypes {
    public static final ActionOnKeyPressPower ACTION_ON_KEY_PRESS = new ActionOnKeyPressPower();

    private PowerTypes() {}

    public static void bootstrap() {
        PowerTypeRegistry.register(Apoli.id("creative_flight"), new CreativeFlightPower());
        PowerTypeRegistry.register(Apoli.id("action_over_time"), new ActionOverTimePower());
        PowerTypeRegistry.register(
            Apoli.id("action_on_key_press"),
            ACTION_ON_KEY_PRESS,
            AliasingOptions.builder().addTypeAlias(Apoli.id("active_self")).build()
        );
    }
}
