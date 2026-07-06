package dev.overgrown.apoli;

import dev.overgrown.apoli.network.payload.ApplyVelocityS2C;
import dev.overgrown.apoli.network.payload.DisguiseUpdateS2C;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.SkillDefsSyncS2C;
import dev.overgrown.apoli.network.payload.SkillStateSyncS2C;
import dev.overgrown.apoli.skill.SkillData;
import dev.overgrown.apoli.skill.SkillDataAttachment;
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

    public static void broadcastDisguise(Entity entity, DisguiseUpdateS2C payload) {
        for (ServerPlayer viewer : PlayerLookup.tracking(entity)) {
            send(viewer, DisguiseUpdateS2C.CHANNEL, payload::write);
        }
        if (entity instanceof ServerPlayer self) {
            send(self, DisguiseUpdateS2C.CHANNEL, payload::write);
        }
    }

    public static void sendDisguise(ServerPlayer recipient, DisguiseUpdateS2C payload) {
        send(recipient, DisguiseUpdateS2C.CHANNEL, payload::write);
    }

    public static void sendSkillDefs(ServerPlayer recipient) {
        boolean legacy = dev.overgrown.apoli.network.ProtocolCompat.useLegacyFormats(recipient);
        if (legacy) {
            dev.overgrown.apoli.network.ProtocolCompat.markSentLegacy(recipient);
        }
        send(recipient, SkillDefsSyncS2C.CHANNEL, SkillDefsSyncS2C.fromCurrent(legacy)::write);
    }

    public static void sendSkillState(ServerPlayer recipient) {
        SkillData data = SkillDataAttachment.get(recipient);
        dev.overgrown.apoli.skill.SkillTrees.Visibility vis =
            dev.overgrown.apoli.skill.SkillTrees.computeVisibility(recipient);
        boolean legacy = dev.overgrown.apoli.network.ProtocolCompat.useLegacyFormats(recipient);
        if (legacy) {
            dev.overgrown.apoli.network.ProtocolCompat.markSentLegacy(recipient);
        }
        SkillStateSyncS2C payload = new SkillStateSyncS2C(
            new java.util.HashMap<>(data.pointsView()), new java.util.HashSet<>(data.purchasedView()),
            vis.hidden(), vis.locked(), legacy);
        send(recipient, SkillStateSyncS2C.CHANNEL, payload::write);
    }

    public static void sendProtocolVersion(ServerPlayer recipient) {
        send(recipient, dev.overgrown.apoli.network.payload.ProtocolVersionPayload.CHANNEL,
            new dev.overgrown.apoli.network.payload.ProtocolVersionPayload(
                dev.overgrown.apoli.network.ProtocolCompat.VERSION)::write);
    }

    
    
    public static void broadcastRopeCreate(net.minecraft.server.level.ServerLevel level, RopeCreateS2C payload) {
        for (ServerPlayer p : level.players()) send(p, RopeCreateS2C.CHANNEL, payload::write);
    }

    public static void broadcastRopeDelete(net.minecraft.server.level.ServerLevel level, RopeDeleteS2C payload) {
        for (ServerPlayer p : level.players()) send(p, RopeDeleteS2C.CHANNEL, payload::write);
    }

    public static void broadcastRopeVerletLength(net.minecraft.server.level.ServerLevel level, RopeVerletLengthS2C payload) {
        for (ServerPlayer p : level.players()) send(p, RopeVerletLengthS2C.CHANNEL, payload::write);
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
