package dev.overgrown.apoli;

import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Single registration entry point for Apoli's network payloads. In 1.21.1
 * Fabric uses {@link PayloadTypeRegistry} for both sides and
 * {@link ServerPlayNetworking#send(ServerPlayer, net.minecraft.network.protocol.common.custom.CustomPacketPayload)}
 * takes the payload directly — no per-call buffer allocation.
 */
public final class ApoliNetwork {
    private ApoliNetwork() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(SyncPowersS2C.TYPE, SyncPowersS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEntityPowersS2C.TYPE, SyncEntityPowersS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PowerActivatedS2C.TYPE, PowerActivatedS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncKeybindsS2C.TYPE, SyncKeybindsS2C.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PowerActivationC2S.TYPE, PowerActivationC2S.STREAM_CODEC);
    }

    public static void sendPowers(ServerPlayer recipient, SyncPowersS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
    }

    public static void sendEntityPowers(ServerPlayer recipient, SyncEntityPowersS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
    }

    public static void sendActivated(ServerPlayer recipient, PowerActivatedS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
    }

    public static void sendKeybinds(ServerPlayer recipient, SyncKeybindsS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
    }

    public static void broadcastPowers(MinecraftServer server, SyncPowersS2C payload) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    public static void broadcastKeybinds(MinecraftServer server, SyncKeybindsS2C payload) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}
