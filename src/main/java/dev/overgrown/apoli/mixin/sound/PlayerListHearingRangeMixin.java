package dev.overgrown.apoli.mixin.sound;

import dev.overgrown.apoli.power.builtin.ModifyHearingRangePower;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListHearingRangeMixin {

    @Shadow @Final private List<ServerPlayer> players;

    @Inject(method = "broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At("HEAD"), cancellable = true)
    private void apoli$applyHearingRange(@Nullable Player except, double x, double y, double z, double radius,
                                         ResourceKey<Level> dimension, Packet<?> packet, CallbackInfo ci) {
        if (!ModifyHearingRangePower.inUse()) return;
        if (!(packet instanceof ClientboundSoundPacket) && !(packet instanceof ClientboundSoundEntityPacket)) return;
        ci.cancel();
        for (int i = 0, n = this.players.size(); i < n; i++) {
            ServerPlayer listener = this.players.get(i);
            if (listener == except || listener.level().dimension() != dimension) continue;
            double range = ModifyHearingRangePower.soundRange(listener, radius);
            if (range <= 0.0) continue;
            double dx = x - listener.getX();
            double dy = y - listener.getY();
            double dz = z - listener.getZ();
            if (dx * dx + dy * dy + dz * dz < range * range) {
                listener.connection.send(packet);
            }
        }
    }
}
