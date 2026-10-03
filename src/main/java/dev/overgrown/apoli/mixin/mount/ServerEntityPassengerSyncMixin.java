package dev.overgrown.apoli.mixin.mount;

import dev.overgrown.apoli.mount.MountOffsets;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityPassengerSyncMixin {
    @Shadow @Final private Entity entity;

    @Inject(method = "sendChanges", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerEntity;removedPassengers(Ljava/util/List;Ljava/util/List;)Ljava/util/stream/Stream;"))
    private void apoli$syncPassengerChange(CallbackInfo ci) {
        if (this.entity instanceof ServerPlayer self && self.connection != null) {
            self.connection.send(new ClientboundSetPassengersPacket(self));
        }
        MountOffsets.syncPassengers(this.entity);
    }

    @Inject(method = "addPairing", at = @At("TAIL"))
    private void apoli$syncPairedMountOffsets(ServerPlayer player, CallbackInfo ci) {
        MountOffsets.syncPairing(player, this.entity);
    }
}
