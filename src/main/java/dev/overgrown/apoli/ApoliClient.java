package dev.overgrown.apoli;

import dev.overgrown.apoli.client.ApoliKeyHandler;
import dev.overgrown.apoli.client.ClientPowerContainer;
import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.client.DynamicKeyMappingManager;
import dev.overgrown.apoli.client.KeyPressWatcher;
import dev.overgrown.apoli.keybind.HeldKeys;
import dev.overgrown.apoli.client.OverlayRenderer;
import dev.overgrown.apoli.client.PhasingRenderState;
import dev.overgrown.apoli.client.PowerHudRenderer;
import dev.overgrown.apoli.client.rope.RopeClientManager;
import dev.overgrown.apoli.client.rope.RopeRenderer;
import dev.overgrown.apoli.client.rope.VerletRopeState;
import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
import dev.overgrown.apoli.network.payload.KeyHeldC2S;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeDeleteS2C;
import dev.overgrown.apoli.network.payload.RopeVerletLengthS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

public final class ApoliClient implements ClientModInitializer {
    private static final net.minecraft.client.KeyMapping SKILL_TREE_KEY = new net.minecraft.client.KeyMapping(
        "key.apoli.skill_tree", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
        org.lwjgl.glfw.GLFW.GLFW_KEY_K, "key.categories.apoli");

    private static final net.minecraft.client.KeyMapping SPEECH_KEY = new net.minecraft.client.KeyMapping(
        "key.apoli.speech_to_action", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
        com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(), "key.categories.apoli");


    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(SKILL_TREE_KEY);
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(SPEECH_KEY);
        dev.overgrown.apoli.client.speech.SpeechClient.setPushToTalkKey(SPEECH_KEY);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.overgrown.apoli.entity.ApoliEntities.CUSTOM_PROJECTILE,
            dev.overgrown.apoli.client.CustomProjectileRenderer::new);

        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            dev.overgrown.apoli.client.summon.SummonModelLayers.MINION,
            dev.overgrown.apoli.client.summon.MinionModel::createBodyLayer);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            dev.overgrown.apoli.client.summon.SummonModelLayers.CLONE,
            () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE, false), 64, 64));
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            dev.overgrown.apoli.client.summon.SummonModelLayers.CLONE_SLIM,
            () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE, true), 64, 64));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.overgrown.apoli.entity.ApoliEntities.MINION,
            dev.overgrown.apoli.client.summon.MinionRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.overgrown.apoli.entity.ApoliEntities.CLONE,
            dev.overgrown.apoli.client.summon.CloneRenderer::new);

        PowerContainerAttachment.setClientLookup(entity ->
            dev.overgrown.apoli.client.ClientPowerState.powersFor(entity.getId()).isEmpty()
                ? null
                : new ClientPowerContainer(entity));

        HeldKeys.setClientLookup((entity, key) ->
            entity == Minecraft.getInstance().player && KeyPressWatcher.isLocalHeld(key));
        KeyPressWatcher.setSender(keys -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new KeyHeldC2S(keys).write(buf);
            ClientPlayNetworking.send(KeyHeldC2S.CHANNEL, buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPowersS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncPowersS2C payload = SyncPowersS2C.read(buf);
            mc.execute(() -> ClientPowerState.applyPowersSync(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.SyncPowersChunkS2C.CHANNEL,
            (mc, handler, buf, sender) -> {
                dev.overgrown.apoli.network.payload.SyncPowersChunkS2C payload =
                    dev.overgrown.apoli.network.payload.SyncPowersChunkS2C.read(buf);
                mc.execute(() -> ClientPowerState.applyPowersChunk(payload));
            });

        ClientPlayNetworking.registerGlobalReceiver(SyncEntityPowersS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncEntityPowersS2C payload = SyncEntityPowersS2C.read(buf);
            mc.execute(() -> ClientPowerState.applyEntityPowersSync(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(PowerActivatedS2C.CHANNEL, (mc, handler, buf, sender) -> {
            PowerActivatedS2C payload = PowerActivatedS2C.read(buf);
            mc.execute(() -> ClientPowerState.setCooldown(payload.power(), payload.cooldown()));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncKeybindsS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncKeybindsS2C payload = SyncKeybindsS2C.read(buf);
            mc.execute(() -> DynamicKeyMappingManager.applyKeybinds(payload.keybinds()));
        });

        ClientPlayNetworking.registerGlobalReceiver(ApplyVelocityS2C.CHANNEL, (mc, handler, buf, sender) -> {
            ApplyVelocityS2C payload = ApplyVelocityS2C.read(buf);
            mc.execute(() -> {
                if (mc.level == null) return;
                net.minecraft.world.entity.Entity e = mc.level.getEntity(payload.entityId());
                if (e == null) return;
                net.minecraft.world.phys.Vec3 delta = new net.minecraft.world.phys.Vec3(payload.x(), payload.y(), payload.z());
                e.setDeltaMovement(payload.set() ? delta : e.getDeltaMovement().add(delta));
            });
        });

        dev.overgrown.apoli.client.disguise.ClientDisguiseManager.install();
        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.DisguiseUpdateS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.DisguiseUpdateS2C payload = dev.overgrown.apoli.network.payload.DisguiseUpdateS2C.read(buf);
            mc.execute(() -> payload.data().ifPresentOrElse(
                data -> dev.overgrown.apoli.client.disguise.ClientDisguiseManager.apply(payload.entityId(), data),
                () -> dev.overgrown.apoli.client.disguise.ClientDisguiseManager.remove(payload.entityId())));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.ProtocolVersionPayload.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.ProtocolVersionPayload payload =
                dev.overgrown.apoli.network.payload.ProtocolVersionPayload.read(buf);
            mc.execute(() -> dev.overgrown.apoli.client.ClientProtocolState.setServerVersion(payload.version()));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.TextDisplayS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.TextDisplayS2C payload = dev.overgrown.apoli.network.payload.TextDisplayS2C.read(buf);
            mc.execute(() -> dev.overgrown.apoli.client.TextOverlayRenderer.apply(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.LabelUpdateS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.LabelUpdateS2C payload = dev.overgrown.apoli.network.payload.LabelUpdateS2C.read(buf);
            mc.execute(() -> dev.overgrown.apoli.client.ClientLabelState.apply(payload.entityId(), payload.texts()));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.ForceKeyS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.ForceKeyS2C payload = dev.overgrown.apoli.network.payload.ForceKeyS2C.read(buf);
            mc.execute(() -> dev.overgrown.apoli.client.ForcedKeys.force(payload.key(), payload.duration(), payload.release()));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new dev.overgrown.apoli.network.payload.ProtocolVersionPayload(
                dev.overgrown.apoli.network.ProtocolCompat.VERSION).write(buf);
            ClientPlayNetworking.send(dev.overgrown.apoli.network.payload.ProtocolVersionPayload.CHANNEL, buf);
            if (dev.overgrown.apoli.client.ApoliClientConfig.get().speechToAction()) {
                dev.overgrown.apoli.client.speech.SpeechClient.onJoin();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.SkillDefsSyncS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.SkillDefsSyncS2C payload = dev.overgrown.apoli.network.payload.SkillDefsSyncS2C.read(buf);
            mc.execute(() -> dev.overgrown.apoli.client.skill.ClientSkillState.applyDefs(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.SkillStateSyncS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.SkillStateSyncS2C payload = dev.overgrown.apoli.network.payload.SkillStateSyncS2C.read(buf);
            mc.execute(() -> dev.overgrown.apoli.client.skill.ClientSkillState.applyState(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.apoli.network.payload.RadialMenuOpenS2C.CHANNEL, (mc, handler, buf, sender) -> {
            dev.overgrown.apoli.network.payload.RadialMenuOpenS2C payload = dev.overgrown.apoli.network.payload.RadialMenuOpenS2C.read(buf);
            mc.execute(() -> mc.setScreen(new dev.overgrown.apoli.client.radial.RadialMenuScreen(payload)));
        });

        ClientPlayNetworking.registerGlobalReceiver(RopeCreateS2C.CHANNEL, (mc, handler, buf, sender) -> {
            RopeCreateS2C payload = RopeCreateS2C.read(buf);
            mc.execute(() -> {
                if (mc.level != null) RopeClientManager.attach(payload, mc.level);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RopeDeleteS2C.CHANNEL, (mc, handler, buf, sender) -> {
            RopeDeleteS2C payload = RopeDeleteS2C.read(buf);
            mc.execute(() -> RopeClientManager.detach(payload.id()));
        });

        ClientPlayNetworking.registerGlobalReceiver(RopeVerletLengthS2C.CHANNEL, (mc, handler, buf, sender) -> {
            RopeVerletLengthS2C payload = RopeVerletLengthS2C.read(buf);
            mc.execute(() -> {
                VerletRopeState rope = RopeClientManager.get(payload.id());
                if (rope != null) rope.targetLength = payload.length();
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) ->
            mc.execute(() -> {
                DynamicKeyMappingManager.unregisterAll();
                ClientPowerState.clear();
                dev.overgrown.apoli.client.TextOverlayRenderer.clear();
                dev.overgrown.apoli.client.ClientLabelState.clear();
                RopeClientManager.clear();
                KeyPressWatcher.reset();
                dev.overgrown.apoli.client.ForcedKeys.clear();
                dev.overgrown.apoli.client.CursorSpeedState.reset();
                dev.overgrown.apoli.client.disguise.ClientDisguiseManager.clear();
                dev.overgrown.apoli.client.skill.ClientSkillState.clear();
                dev.overgrown.apoli.compat.figura.FiguraModelPowerManager.clear();
                dev.overgrown.apoli.client.ClientProtocolState.reset();
                dev.overgrown.apoli.client.speech.SpeechClient.onLeave();
            }));

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
            .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.resources.ResourceLocation getFabricId() {
                    return Apoli.id("figura_avatars");
                }

                @Override
                public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                    dev.overgrown.apoli.compat.figura.FiguraModelPowerManager.onResourcesReloaded();
                }
            });

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
            .registerReloadListener(dev.overgrown.apoli.client.render.CustomModelManager.INSTANCE);

        if (dev.overgrown.apoli.compat.ModCompat.LAMBDYNLIGHTS) {
            dev.overgrown.apoli.compat.lambdynlights.LambDynamicLightsCompat.init();
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            dev.overgrown.apoli.client.CursorSpeedState.tick(mc);
            if (mc.player != null && !mc.isPaused()) {
                ApoliKeyHandler.onClientTick();
                while (SKILL_TREE_KEY.consumeClick()) {
                    if (mc.screen == null && dev.overgrown.apoli.client.skill.ClientSkillState.hasAnyTree()) {
                        if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(dev.overgrown.apoli.network.payload.RequestSkillStateC2S.CHANNEL)) {
                            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(dev.overgrown.apoli.network.payload.RequestSkillStateC2S.CHANNEL, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create());
                        }
                        mc.setScreen(new dev.overgrown.apoli.client.skill.SkillTreeScreen());
                    }
                }
            }
            PhasingRenderState.clientTick(mc);
            RopeClientManager.tick();
            dev.overgrown.apoli.client.TextOverlayRenderer.tick();
            dev.overgrown.apoli.client.disguise.ClientDisguiseManager.tick(mc);
            dev.overgrown.apoli.client.speech.SpeechClient.clientTick(mc);
            if (dev.overgrown.apoli.compat.ModCompat.FIGURA) {
                dev.overgrown.apoli.compat.figura.FiguraModelPowerManager.tick(mc);
            }
            dev.overgrown.apoli.client.PlayerModelTypeReporter.tick(mc);
            dev.overgrown.apoli.client.ForcedKeys.tick();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(RopeRenderer::render);

        HudRenderCallback.EVENT.register(OverlayRenderer::renderBelowHud);
        HudRenderCallback.EVENT.register(PowerHudRenderer::render);
        HudRenderCallback.EVENT.register(dev.overgrown.apoli.client.TextOverlayRenderer::render);
        HudRenderCallback.EVENT.register(OverlayRenderer::renderAboveHud);
    }
}
