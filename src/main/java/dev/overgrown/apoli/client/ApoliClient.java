package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.client.rope.RopeClientManager;
import dev.overgrown.apoli.client.rope.RopeRenderer;
import dev.overgrown.apoli.entity.ApoliEntities;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Apoli.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ApoliClient {
    private ApoliClient() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(Apoli.id("overlay_below_hud"),
            (graphics, deltaTracker) -> OverlayRenderer.renderBelowHud(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAbove(VanillaGuiLayers.HOTBAR, Apoli.id("power_hud"),
            (graphics, deltaTracker) -> PowerHudRenderer.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(Apoli.id("overlay_above_hud"),
            (graphics, deltaTracker) -> OverlayRenderer.renderAboveHud(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        NeoForge.EVENT_BUS.register(GameBus.class);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ApoliEntities.CUSTOM_PROJECTILE.get(), CustomProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PowerContainerAttachment.setClientLookup(entity ->
            ClientPowerState.powersFor(entity.getId()).isEmpty() ? null : new ClientPowerContainer(entity)));
    }

    public static final class GameBus {
        private GameBus() {}

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            PhasingRenderState.clientTick(mc);
            RopeClientManager.tick();
            if (mc.player == null || mc.isPaused()) return;
            ApoliKeyHandler.onClientTick();
        }

        @SubscribeEvent
        public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            DynamicKeyMappingManager.unregisterAll();
            ClientPowerState.clear();
            RopeClientManager.clear();
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            RopeRenderer.render(event);
        }

        @SubscribeEvent
        public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
            if (event.getOverlayType() != RenderBlockScreenEffectEvent.OverlayType.BLOCK) return;
            Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera instanceof LivingEntity living && PowerLookup.hasActive(living, Apoli.id("phasing"))) {
                event.setCanceled(true);
            }
        }
    }
}
