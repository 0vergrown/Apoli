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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class ApoliNetwork {
    private ApoliNetwork() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(SyncPowersS2C.TYPE, SyncPowersS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEntityPowersS2C.TYPE, SyncEntityPowersS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PowerActivatedS2C.TYPE, PowerActivatedS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncKeybindsS2C.TYPE, SyncKeybindsS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ApplyVelocityS2C.TYPE, ApplyVelocityS2C.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PowerActivationC2S.TYPE, PowerActivationC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PowerToggleC2S.TYPE, PowerToggleC2S.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(RopeCreateS2C.TYPE, RopeCreateS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RopeDeleteS2C.TYPE, RopeDeleteS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RopeVerletLengthS2C.TYPE, RopeVerletLengthS2C.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RopeChangeLengthC2S.TYPE, RopeChangeLengthC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RopeSwingC2S.TYPE, RopeSwingC2S.STREAM_CODEC);
    }

    public static void sendRopeCreate(ServerPlayer recipient, RopeCreateS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
    }

    public static void sendRopeDelete(ServerPlayer recipient, RopeDeleteS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
    }

    public static void sendRopeVerletLength(ServerPlayer recipient, RopeVerletLengthS2C payload) {
        ServerPlayNetworking.send(recipient, payload);
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

    public static void sendApplyVelocityToTrackers(Entity entity, ApplyVelocityS2C payload) {
        for (ServerPlayer viewer : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(viewer, payload);
        }
        if (entity instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
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
