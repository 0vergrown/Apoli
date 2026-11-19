package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.network.payload.JumpC2S;
import dev.overgrown.apoli.power.builtin.ModifyJumpHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerJumpSyncMixin {
    @Inject(method = "jumpFromGround()V", at = @At("TAIL"))
    private void apoli$sendJumpToServer(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && ModifyJumpHandler.hasJumpActions(player)
            && player.connection.hasChannel(JumpC2S.TYPE)) {
            PacketDistributor.sendToServer(JumpC2S.INSTANCE);
        }
    }
}
