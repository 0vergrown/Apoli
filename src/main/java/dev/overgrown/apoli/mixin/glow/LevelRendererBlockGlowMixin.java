package dev.overgrown.apoli.mixin.glow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.glow.BlockGlowRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LevelRendererBlockGlowMixin {

    @Unique
    private static final Matrix4f apoli$IDENTITY = new Matrix4f();

    @Shadow
    private @Nullable PostChain entityEffect;

    @Shadow
    protected abstract boolean shouldShowEntityOutlines();

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void apoli$beginBlockGlows(CallbackInfo ci) {
        BlockGlowRenderer.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void apoli$renderBlockGlowOutlines(CallbackInfo ci) {
        BlockGlowRenderer.renderOutlines(shouldShowEntityOutlines(), apoli$IDENTITY);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = At.Shift.AFTER))
    private void apoli$processBlockGlowOutlines(CallbackInfo ci) {
        if (!BlockGlowRenderer.needsOutlineEffect() || entityEffect == null || !shouldShowEntityOutlines()) return;
        entityEffect.process(0.0F);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void apoli$renderBlockGlowXray(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
                                           GameRenderer gameRenderer, LightTexture lightTexture,
                                           Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(frustumMatrix);
        RenderSystem.applyModelViewMatrix();
        BlockGlowRenderer.renderXray(new PoseStack(), deltaTracker.getGameTimeDeltaPartialTick(false));
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
