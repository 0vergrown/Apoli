package dev.overgrown.apoli;

import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.SyncEntityPowersS2C;
import dev.overgrown.apoli.network.payload.SyncKeybindsS2C;
import dev.overgrown.apoli.network.payload.SyncPowersS2C;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Thin wrapper around {@link ServerPlayNetworking} that produces packed
 * {@link FriendlyByteBuf}s for Apoli's S2C payload records.
 */
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
