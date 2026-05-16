package dev.overgrown.apoli.client;

import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public final class ApoliKeyHandler {
    private ApoliKeyHandler() {}

    public static void onClientTick() {
        ClientPowerState.tickCooldowns();
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof ActionOnKeyPressPower)) continue;
            if (!(power.config() instanceof ActionOnKeyPressPower.Config cfg)) continue;
            KeyMapping km = ApoliKeyMappings.resolve(cfg.key().key());
            if (km == null) continue;
            boolean trigger;
            if (cfg.key().continuous()) {
                trigger = km.isDown();
                while (km.consumeClick()) { /* drain so it doesn't re-fire next tick */ }
            } else {
                trigger = false;
                while (km.consumeClick()) trigger = true;
            }
            if (!trigger) continue;
            if (ClientPowerState.getCooldown(powerId) > 0) continue;
            ClientPlayNetworking.send(new PowerActivationC2S(powerId));
        }
    }
}
