package dev.overgrown.apoli.client;

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
        ClientPlayNetworking.registerGlobalReceiver(SyncPowersS2C.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPowerState.applyPowersSync(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SyncEntityPowersS2C.TYPE, (payload, context) ->
            context.client().execute(() -> {
                LocalPlayer p = context.client().player;
                if (p != null) ClientPowerState.applyEntityPowersSync(payload, p.getId());
            }));

        ClientPlayNetworking.registerGlobalReceiver(PowerActivatedS2C.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPowerState.setCooldown(payload.power(), payload.cooldown())));

        ClientPlayNetworking.registerGlobalReceiver(SyncKeybindsS2C.TYPE, (payload, context) ->
            context.client().execute(() -> DynamicKeyMappingManager.applyKeybinds(payload.keybinds())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) ->
            mc.execute(DynamicKeyMappingManager::unregisterAll));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player != null && !mc.isPaused()) ApoliKeyHandler.onClientTick();
        });

        HudRenderCallback.EVENT.register(PowerHudRenderer::render);
    }
}
