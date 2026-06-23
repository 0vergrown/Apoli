package dev.overgrown.apoli.client;

import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.PowerToggleC2S;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public final class ApoliKeyHandler {
    private ApoliKeyHandler() {}

    public static void onClientTick() {
        ClientPowerState.tickCooldowns();
        ApoliKeyMappings.beginTick();
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            PowerType<?> type = PowerTypeRegistry.get(power.typeId());
            if (type instanceof ActionOnKeyPressPower) {
                if (!(power.config() instanceof ActionOnKeyPressPower.Config cfg)) continue;
                KeyMapping km = ApoliKeyMappings.resolve(cfg.key().key());
                if (km == null || !ApoliKeyMappings.consumePress(km, cfg.key().continuous())) continue;
                if (ClientPowerState.getCooldown(powerId) > 0) continue;
                ClientPlayNetworking.send(new PowerActivationC2S(powerId));
            } else if (type instanceof FireProjectilePower) {
                if (!(power.config() instanceof FireProjectilePower.Config cfg)) continue;
                Key fpKey = cfg.params().key().orElse(Key.DEFAULT_PRIMARY);
                KeyMapping km = ApoliKeyMappings.resolve(fpKey.key());
                if (km == null || !ApoliKeyMappings.consumePress(km, fpKey.continuous())) continue;
                if (ClientPowerState.getCooldown(powerId) > 0) continue;
                ClientPlayNetworking.send(new PowerActivationC2S(powerId));
            } else if (type instanceof InventoryPower) {
                if (!(power.config() instanceof InventoryPower.Config cfg)) continue;
                KeyMapping km = ApoliKeyMappings.resolve(cfg.key().key());
                if (km == null || !ApoliKeyMappings.consumePress(km, cfg.key().continuous())) continue;
                ClientPlayNetworking.send(new PowerActivationC2S(powerId));
            } else if (type instanceof TogglePower) {
                if (!(power.config() instanceof TogglePower.Config cfg)) continue;
                KeyMapping km = ApoliKeyMappings.resolve(cfg.key().key());
                if (km == null || !ApoliKeyMappings.consumePress(km, false)) continue;
                ClientPlayNetworking.send(new PowerToggleC2S(powerId));
            }
        }
    }
}
