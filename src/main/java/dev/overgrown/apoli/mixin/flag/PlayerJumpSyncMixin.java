package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.network.payload.JumpC2S;
import dev.overgrown.apoli.power.builtin.ModifyJumpHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerJumpSyncMixin {
    @Inject(method = "jumpFromGround()V", at = @At("TAIL"))
    private void apoli$sendJumpToServer(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && ModifyJumpHandler.hasJumpActions(player)
            && ClientPlayNetworking.canSend(JumpC2S.TYPE)) {
            ClientPlayNetworking.send(JumpC2S.INSTANCE);
        }
    }
}
