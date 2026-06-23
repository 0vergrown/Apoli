package dev.overgrown.apoli;

import dev.overgrown.apoli.client.ApoliKeyHandler;
import dev.overgrown.apoli.client.ClientPowerContainer;
import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.client.DynamicKeyMappingManager;
import dev.overgrown.apoli.client.OverlayRenderer;
import dev.overgrown.apoli.client.PhasingRenderState;
import dev.overgrown.apoli.client.PowerHudRenderer;
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
            dev.overgrown.apoli.client.ClientPowerState.powersFor(entity.getId()).isEmpty()
                ? null
                : new ClientPowerContainer(entity));

        ClientPlayNetworking.registerGlobalReceiver(SyncPowersS2C.CHANNEL, (mc, handler, buf, sender) -> {
            SyncPowersS2C payload = SyncPowersS2C.read(buf);
            mc.execute(() -> ClientPowerState.applyPowersSync(payload));
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

        ClientPlayNetworking.registerGlobalReceiver(RopeCreateS2C.CHANNEL, (mc, handler, buf, sender) -> {
            RopeCreateS2C payload = RopeCreateS2C.read(buf);
            mc.execute(() -> RopeClientManager.attach(
                payload.owner(), payload.anchor(), payload.length(), payload.maxLength(), payload.texture()));
        });

        ClientPlayNetworking.registerGlobalReceiver(RopeDeleteS2C.CHANNEL, (mc, handler, buf, sender) -> {
            RopeDeleteS2C payload = RopeDeleteS2C.read(buf);
            mc.execute(() -> RopeClientManager.detach(payload.owner()));
        });

        ClientPlayNetworking.registerGlobalReceiver(RopeVerletLengthS2C.CHANNEL, (mc, handler, buf, sender) -> {
            RopeVerletLengthS2C payload = RopeVerletLengthS2C.read(buf);
            mc.execute(() -> {
                VerletRopeState rope = RopeClientManager.get(payload.owner());
                if (rope != null) rope.targetLength = payload.length();
            });
        });

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

        HudRenderCallback.EVENT.register(OverlayRenderer::renderBelowHud);
        HudRenderCallback.EVENT.register(PowerHudRenderer::render);
        HudRenderCallback.EVENT.register(OverlayRenderer::renderAboveHud);
    }
}
