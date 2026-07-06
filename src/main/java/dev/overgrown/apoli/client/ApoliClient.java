package dev.overgrown.apoli.client;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.network.payload.KeyHeldC2S;
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
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Apoli.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ApoliClient {
    private ApoliClient() {}

    public static final net.minecraft.client.KeyMapping SKILL_TREE_KEY = new net.minecraft.client.KeyMapping(
        "key.apoli.skill_tree", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
        org.lwjgl.glfw.GLFW.GLFW_KEY_K, "key.categories.apoli");

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(SKILL_TREE_KEY);
    }

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
        event.registerEntityRenderer(ApoliEntities.MINION.get(), dev.overgrown.apoli.client.summon.MinionRenderer::new);
        event.registerEntityRenderer(ApoliEntities.CLONE.get(), dev.overgrown.apoli.client.summon.CloneRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(dev.overgrown.apoli.client.summon.SummonModelLayers.MINION,
            dev.overgrown.apoli.client.summon.MinionModel::createBodyLayer);
        event.registerLayerDefinition(dev.overgrown.apoli.client.summon.SummonModelLayers.CLONE,
            () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE, false), 64, 64));
        event.registerLayerDefinition(dev.overgrown.apoli.client.summon.SummonModelLayers.CLONE_SLIM,
            () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE, true), 64, 64));
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager ->
            dev.overgrown.apoli.compat.figura.FiguraModelPowerManager.onResourcesReloaded());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PowerContainerAttachment.setClientLookup(entity ->
                ClientPowerState.powersFor(entity.getId()).isEmpty() ? null : new ClientPowerContainer(entity));
            HeldKeys.setClientLookup((entity, key) ->
                entity == Minecraft.getInstance().player && KeyPressWatcher.isLocalHeld(key));
            KeyPressWatcher.setSender(keys -> PacketDistributor.sendToServer(new KeyHeldC2S(keys)));
            dev.overgrown.apoli.client.disguise.ClientDisguiseManager.install();
        });
    }

    public static final class GameBus {
        private GameBus() {}

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            PhasingRenderState.clientTick(mc);
            RopeClientManager.tick();
            if (dev.overgrown.apoli.compat.ModCompat.FIGURA) {
                dev.overgrown.apoli.compat.figura.FiguraModelPowerManager.tick(mc);
            }
            if (mc.player == null || mc.isPaused()) return;
            ApoliKeyHandler.onClientTick();
            while (SKILL_TREE_KEY.consumeClick()) {
                if (mc.screen == null && dev.overgrown.apoli.client.skill.ClientSkillState.hasAnyTree()) {
                    mc.setScreen(new dev.overgrown.apoli.client.skill.SkillTreeScreen());
                }
            }
        }

        @SubscribeEvent
        public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            DynamicKeyMappingManager.unregisterAll();
            ClientPowerState.clear();
            RopeClientManager.clear();
            KeyPressWatcher.reset();
            dev.overgrown.apoli.client.disguise.ClientDisguiseManager.clear();
            dev.overgrown.apoli.client.skill.ClientSkillState.clear();
            dev.overgrown.apoli.compat.figura.FiguraModelPowerManager.clear();
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
