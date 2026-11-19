package dev.overgrown.apoli.mixin.possession;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.possession.ClientPossession;
import dev.overgrown.apoli.client.possession.PossessionArms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererPossessionMixin {

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void apoli$hostRightArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                    CallbackInfo ci) {
        if (ClientPossession.active() && PossessionArms.render(pose, buffers, light, player, true)) ci.cancel();
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void apoli$hostLeftArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                   CallbackInfo ci) {
        if (ClientPossession.active() && PossessionArms.render(pose, buffers, light, player, false)) ci.cancel();
    }
}
