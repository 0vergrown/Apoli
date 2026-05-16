package dev.overgrown.apoli;

import dev.overgrown.apoli.client.ApoliKeyHandler;
import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.client.DynamicKeyMappingManager;
import dev.overgrown.apoli.client.PowerHudRenderer;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.player.LocalPlayer;

public final class ApoliClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncPowersS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncPowersS2C payload = SyncPowersS2C.read(buf);
            mc.execute(() -> ClientPowerState.applyPowersSync(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncEntityPowersS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncEntityPowersS2C payload = SyncEntityPowersS2C.read(buf);
            mc.execute(() -> {
                LocalPlayer p = mc.player;
                if (p != null) ClientPowerState.applyEntityPowersSync(payload, p.getId());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PowerActivatedS2C.CHANNEL, (mc, handler, buf, sender) -> {
            PowerActivatedS2C payload = PowerActivatedS2C.read(buf);
            mc.execute(() -> ClientPowerState.setCooldown(payload.power(), payload.cooldown()));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncKeybindsS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncKeybindsS2C payload = SyncKeybindsS2C.read(buf);
            mc.execute(() -> DynamicKeyMappingManager.applyKeybinds(payload.keybinds()));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) ->
            mc.execute(DynamicKeyMappingManager::unregisterAll));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player != null && !mc.isPaused()) ApoliKeyHandler.onClientTick();
        });

        HudRenderCallback.EVENT.register(PowerHudRenderer::render);
    }
}
