package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Mod-bus subscriber for client-side registration events (GUI layers). Game-bus
 * runtime hooks (client tick, disconnect) live in the nested {@link GameBus}
 * class and are registered explicitly from {@link #onRegisterGuiLayers} —
 * NeoForge's annotation processor doesn't reliably discover nested static
 * subscriber classes in 21.1.x, so the explicit registration is the
 * safe path. (This mirrors what the Forge 1.20.1 port does.)
 */
@EventBusSubscriber(modid = Apoli.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ApoliClient {
    private ApoliClient() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Apoli.id("power_hud"),
            (graphics, deltaTracker) -> PowerHudRenderer.render(graphics, deltaTracker));
        NeoForge.EVENT_BUS.register(GameBus.class);
    }

    public static final class GameBus {
        private GameBus() {}

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.isPaused()) return;
            ApoliKeyHandler.onClientTick();
        }

        @SubscribeEvent
        public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            DynamicKeyMappingManager.unregisterAll();
        }
    }
}
