package dev.overgrown.apoli;

import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge's PayloadRegistrar plays the same role as Fabric's
 * PayloadTypeRegistry — register one payload Type + StreamCodec + handler
 * per message. Sending uses {@link PacketDistributor}.
 */
public final class ApoliNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ApoliNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Apoli.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToClient(SyncPowersS2C.TYPE, SyncPowersS2C.STREAM_CODEC, ApoliNetwork::onSyncPowers);
        registrar.playToClient(SyncEntityPowersS2C.TYPE, SyncEntityPowersS2C.STREAM_CODEC, ApoliNetwork::onSyncEntityPowers);
        registrar.playToClient(PowerActivatedS2C.TYPE, PowerActivatedS2C.STREAM_CODEC, ApoliNetwork::onPowerActivated);
        registrar.playToClient(SyncKeybindsS2C.TYPE, SyncKeybindsS2C.STREAM_CODEC, ApoliNetwork::onSyncKeybinds);
        registrar.playToServer(PowerActivationC2S.TYPE, PowerActivationC2S.STREAM_CODEC, ApoliNetwork::onPowerActivation);
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

    private static void onPowerActivation(PowerActivationC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                Apoli.handleActivation(sp, payload);
            }
        });
    }

    // ---- Send helpers --------------------------------------------------------

    public static void sendPowers(ServerPlayer recipient, SyncPowersS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendEntityPowers(ServerPlayer recipient, SyncEntityPowersS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendActivated(ServerPlayer recipient, PowerActivatedS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void sendKeybinds(ServerPlayer recipient, SyncKeybindsS2C payload) {
        PacketDistributor.sendToPlayer(recipient, payload);
    }

    public static void broadcastPowers(MinecraftServer server, SyncPowersS2C payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void broadcastKeybinds(MinecraftServer server, SyncKeybindsS2C payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
