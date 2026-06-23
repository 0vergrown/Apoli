package dev.overgrown.apoli.client;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.client.rope.RopeClientManager;
import dev.overgrown.apoli.client.rope.RopeRenderer;
import dev.overgrown.apoli.client.rope.VerletRopeState;
import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
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

public final class ApoliClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.overgrown.apoli.entity.ApoliEntities.CUSTOM_PROJECTILE,
            dev.overgrown.apoli.client.CustomProjectileRenderer::new);

        PowerContainerAttachment.setClientLookup(entity ->
            ClientPowerState.powersFor(entity.getId()).isEmpty()
                ? null
                : new ClientPowerContainer(entity));

        ClientPlayNetworking.registerGlobalReceiver(SyncPowersS2C.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPowerState.applyPowersSync(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SyncEntityPowersS2C.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPowerState.applyEntityPowersSync(payload)));

        ClientPlayNetworking.registerGlobalReceiver(PowerActivatedS2C.TYPE, (payload, context) ->
            context.client().execute(() -> ClientPowerState.setCooldown(payload.power(), payload.cooldown())));

        ClientPlayNetworking.registerGlobalReceiver(SyncKeybindsS2C.TYPE, (payload, context) ->
            context.client().execute(() -> DynamicKeyMappingManager.applyKeybinds(payload.keybinds())));

        ClientPlayNetworking.registerGlobalReceiver(ApplyVelocityS2C.TYPE, (payload, context) ->
            context.client().execute(() -> {
                if (context.client().level == null) return;
                net.minecraft.world.entity.Entity e = context.client().level.getEntity(payload.entityId());
                if (e == null) return;
                net.minecraft.world.phys.Vec3 delta = new net.minecraft.world.phys.Vec3(payload.x(), payload.y(), payload.z());
                e.setDeltaMovement(payload.set() ? delta : e.getDeltaMovement().add(delta));
            }));

        ClientPlayNetworking.registerGlobalReceiver(RopeCreateS2C.TYPE, (payload, context) ->
            context.client().execute(() -> RopeClientManager.attach(
                payload.owner(), payload.anchor(), payload.length(), payload.maxLength(), payload.texture())));

        ClientPlayNetworking.registerGlobalReceiver(RopeDeleteS2C.TYPE, (payload, context) ->
            context.client().execute(() -> RopeClientManager.detach(payload.owner())));

        ClientPlayNetworking.registerGlobalReceiver(RopeVerletLengthS2C.TYPE, (payload, context) ->
            context.client().execute(() -> {
                VerletRopeState rope = RopeClientManager.get(payload.owner());
                if (rope != null) rope.targetLength = payload.length();
            }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) ->
            mc.execute(() -> {
                DynamicKeyMappingManager.unregisterAll();
                ClientPowerState.clear();
                RopeClientManager.clear();
            }));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player != null && !mc.isPaused()) ApoliKeyHandler.onClientTick();
            PhasingRenderState.clientTick(mc);
            RopeClientManager.tick();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(RopeRenderer::render);

        HudRenderCallback.EVENT.register((gfx, tracker) -> OverlayRenderer.renderBelowHud(gfx, tracker.getGameTimeDeltaPartialTick(false)));
        HudRenderCallback.EVENT.register((gfx, tracker) -> PowerHudRenderer.render(gfx, tracker.getGameTimeDeltaPartialTick(false)));
        HudRenderCallback.EVENT.register((gfx, tracker) -> OverlayRenderer.renderAboveHud(gfx, tracker.getGameTimeDeltaPartialTick(false)));
    }
}
