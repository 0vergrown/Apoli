package dev.overgrown.apoli.mixin.disguise;

import dev.overgrown.apoli.client.disguise.ClientDisguiseManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
@OnlyIn(Dist.CLIENT)
public abstract class ClientPacketListenerDisguiseEventMixin {

    @Shadow
    private ClientLevel level;

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void apoli$forwardEventToDisguise(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (this.level == null) return;
        Entity entity = packet.getEntity(this.level);
        if (entity == null) return;
        ClientDisguiseManager.onEntityEvent(entity, packet.getEventId());
    }
}
