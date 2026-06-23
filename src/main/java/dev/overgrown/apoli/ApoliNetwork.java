package dev.overgrown.apoli;

import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeDeleteS2C;
import dev.overgrown.apoli.network.payload.RopeVerletLengthS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class ApoliNetwork {
    private ApoliNetwork() {}

    public static void sendPowers(ServerPlayer recipient, SyncPowersS2C payload) {
        send(recipient, SyncPowersS2C.CHANNEL, payload::write);
    }

    public static void sendEntityPowers(ServerPlayer recipient, SyncEntityPowersS2C payload) {
        send(recipient, SyncEntityPowersS2C.CHANNEL, payload::write);
    }

    public static void sendActivated(ServerPlayer recipient, PowerActivatedS2C payload) {
        send(recipient, PowerActivatedS2C.CHANNEL, payload::write);
    }

    public static void sendKeybinds(ServerPlayer recipient, SyncKeybindsS2C payload) {
        send(recipient, SyncKeybindsS2C.CHANNEL, payload::write);
    }

    public static void sendApplyVelocityToTrackers(Entity entity, ApplyVelocityS2C payload) {
        for (ServerPlayer viewer : PlayerLookup.tracking(entity)) {
            send(viewer, ApplyVelocityS2C.CHANNEL, payload::write);
        }
        if (entity instanceof ServerPlayer self) {
            send(self, ApplyVelocityS2C.CHANNEL, payload::write);
        }
    }

    public static void sendRopeCreate(ServerPlayer recipient, RopeCreateS2C payload) {
        send(recipient, RopeCreateS2C.CHANNEL, payload::write);
    }

    public static void sendRopeDelete(ServerPlayer recipient, RopeDeleteS2C payload) {
        send(recipient, RopeDeleteS2C.CHANNEL, payload::write);
    }

    public static void sendRopeVerletLength(ServerPlayer recipient, RopeVerletLengthS2C payload) {
        send(recipient, RopeVerletLengthS2C.CHANNEL, payload::write);
    }

    public static void broadcastPowers(MinecraftServer server, SyncPowersS2C payload) {
        broadcast(server, SyncPowersS2C.CHANNEL, payload::write);
    }

    public static void broadcastKeybinds(MinecraftServer server, SyncKeybindsS2C payload) {
        broadcast(server, SyncKeybindsS2C.CHANNEL, payload::write);
    }

    private static void send(ServerPlayer recipient, ResourceLocation channel, java.util.function.Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buf);
        ServerPlayNetworking.send(recipient, channel, buf);
    }

    private static void broadcast(MinecraftServer server, ResourceLocation channel, java.util.function.Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf prototype = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(prototype);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, channel, new FriendlyByteBuf(prototype.copy()));
        }
    }
}
