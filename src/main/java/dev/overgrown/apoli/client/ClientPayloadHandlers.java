package dev.overgrown.apoli.client;

import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void onSyncPowers(SyncPowersS2C msg) {
        ClientPowerState.applyPowersSync(msg);
    }

    public static void onSyncEntityPowers(SyncEntityPowersS2C msg) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) ClientPowerState.applyEntityPowersSync(msg, p.getId());
    }

    public static void onPowerActivated(PowerActivatedS2C msg) {
        ClientPowerState.setCooldown(msg.power(), msg.cooldown());
    }

    public static void onSyncKeybinds(SyncKeybindsS2C msg) {
        DynamicKeyMappingManager.applyKeybinds(msg.keybinds());
    }
}
