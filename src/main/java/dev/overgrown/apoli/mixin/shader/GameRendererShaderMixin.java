package dev.overgrown.apoli.mixin.shader;

import dev.overgrown.apoli.client.ShaderPowerState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererShaderMixin implements ShaderPowerState.GameRendererAccess {

    @Override
    public @Nullable PostChain apoli$loadDetachedEffect(ResourceLocation shader) {
        GameRendererShaderAccessor self = (GameRendererShaderAccessor) this;
        PostChain shared = self.apoli$getPostEffect();
        boolean sharedActive = self.apoli$isEffectActive();
        self.apoli$setPostEffect(null);
        try {
            self.apoli$invokeLoadEffect(shader);
            return self.apoli$getPostEffect();
        } finally {
            self.apoli$setPostEffect(shared);
            self.apoli$setEffectActive(sharedActive);
        }
    }

    @Inject(method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private void apoli$processShaderPower(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        ShaderPowerState.process(this, deltaTracker.getGameTimeDeltaTicks());
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void apoli$resizeShaderPower(int width, int height, CallbackInfo ci) {
        ShaderPowerState.resize(width, height);
    }

    @Inject(method = "togglePostEffect", at = @At("HEAD"))
    private void apoli$toggleShaderPower(CallbackInfo ci) {
        ShaderPowerState.toggle();
    }
}
