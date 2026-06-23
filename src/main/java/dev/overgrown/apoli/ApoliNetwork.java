package dev.overgrown.apoli;

import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.PowerToggleC2S;
import dev.overgrown.apoli.network.payload.RopeChangeLengthC2S;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeDeleteS2C;
import dev.overgrown.apoli.network.payload.RopeSwingC2S;
import dev.overgrown.apoli.network.payload.RopeVerletLengthS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ApoliNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ApoliNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Apoli.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToClient(SyncPowersS2C.TYPE, SyncPowersS2C.STREAM_CODEC, ApoliNetwork::onSyncPowers);
        registrar.playToClient(SyncEntityPowersS2C.TYPE, SyncEntityPowersS2C.STREAM_CODEC, ApoliNetwork::onSyncEntityPowers);
        registrar.playToClient(PowerActivatedS2C.TYPE, PowerActivatedS2C.STREAM_CODEC, ApoliNetwork::onPowerActivated);
        registrar.playToClient(SyncKeybindsS2C.TYPE, SyncKeybindsS2C.STREAM_CODEC, ApoliNetwork::onSyncKeybinds);
        registrar.playToClient(ApplyVelocityS2C.TYPE, ApplyVelocityS2C.STREAM_CODEC, ApoliNetwork::onApplyVelocity);
        registrar.playToServer(PowerActivationC2S.TYPE, PowerActivationC2S.STREAM_CODEC, ApoliNetwork::onPowerActivation);
        registrar.playToServer(PowerToggleC2S.TYPE, PowerToggleC2S.STREAM_CODEC, ApoliNetwork::onPowerToggle);

        registrar.playToClient(RopeCreateS2C.TYPE, RopeCreateS2C.STREAM_CODEC, ApoliNetwork::onRopeCreate);
        registrar.playToClient(RopeDeleteS2C.TYPE, RopeDeleteS2C.STREAM_CODEC, ApoliNetwork::onRopeDelete);
        registrar.playToClient(RopeVerletLengthS2C.TYPE, RopeVerletLengthS2C.STREAM_CODEC, ApoliNetwork::onRopeVerletLength);
        registrar.playToServer(RopeChangeLengthC2S.TYPE, RopeChangeLengthC2S.STREAM_CODEC, ApoliNetwork::onRopeChangeLength);
        registrar.playToServer(RopeSwingC2S.TYPE, RopeSwingC2S.STREAM_CODEC, ApoliNetwork::onRopeSwing);
    }

    private static void onSyncPowers(SyncPowersS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onSyncPowers(payload));
    }

    private static void onSyncEntityPowers(SyncEntityPowersS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onSyncEntityPowers(payload));
    }

    private static void onPowerActivated(PowerActivatedS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onPowerActivated(payload));
    }

    private static void onSyncKeybinds(SyncKeybindsS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onSyncKeybinds(payload));
    }

    private static void onApplyVelocity(ApplyVelocityS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onApplyVelocity(payload));
    }

    private static void onPowerActivation(PowerActivationC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                Apoli.handleActivation(sp, payload);
            }
        });
    }

    private static void onPowerToggle(PowerToggleC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                Apoli.handleToggle(sp, payload);
            }
        });
    }

    private static void onRopeCreate(RopeCreateS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onRopeCreate(payload));
    }

    private static void onRopeDelete(RopeDeleteS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onRopeDelete(payload));
    }

    private static void onRopeVerletLength(RopeVerletLengthS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> dev.overgrown.apoli.client.ClientPayloadHandlers.onRopeVerletLength(payload));
    }

    private static void onRopeChangeLength(RopeChangeLengthC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                dev.overgrown.apoli.rope.RopeManager.handleLengthChange(sp, payload.delta());
            }
        });
    }

    private static void onRopeSwing(RopeSwingC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                dev.overgrown.apoli.rope.RopeManager.applySwingInput(sp, payload.inputDir());
            }
        });
    }

    public static void sendPowers(ServerPlayer recipient, SyncPowersS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendRopeCreate(ServerPlayer recipient, RopeCreateS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendRopeDelete(ServerPlayer recipient, RopeDeleteS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendRopeVerletLength(ServerPlayer recipient, RopeVerletLengthS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendEntityPowers(ServerPlayer recipient, SyncEntityPowersS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendEntityPowersToTrackers(Entity entity, SyncEntityPowersS2C payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    public static void sendEntityPowersToTrackersAndSelf(Entity entity, SyncEntityPowersS2C payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void sendActivated(ServerPlayer recipient, PowerActivatedS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendKeybinds(ServerPlayer recipient, SyncKeybindsS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendApplyVelocityToTrackers(Entity entity, ApplyVelocityS2C payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void broadcastPowers(MinecraftServer server, SyncPowersS2C payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void broadcastKeybinds(MinecraftServer server, SyncKeybindsS2C payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
