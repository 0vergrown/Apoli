package dev.overgrown.apoli.mixin.firstperson;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.particle.RenderedPoses;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ItemInHandRendererHandRootMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void apoli$markHandRoot(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                    LocalPlayer player, int light, CallbackInfo ci) {
        RenderedPoses.beginHands(poseStack);
    }
}
