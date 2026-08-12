package dev.overgrown.apoli.mixin.shader;

import dev.overgrown.apoli.client.ShaderPowerState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererShaderMixin implements ShaderPowerState.GameRendererAccess {

    @Override
    public void apoli$loadEffect(ResourceLocation shader) {
        ((GameRendererShaderAccessor) this).apoli$invokeLoadEffect(shader);
    }

    @Override
    public void apoli$shutdownEffect() {
        ((GameRenderer) (Object) this).shutdownEffect();
    }

    @Override
    public PostChain apoli$postEffect() {
        return ((GameRendererShaderAccessor) this).apoli$getPostEffect();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void apoli$applyShaderPowers(float f, long l, boolean bl, CallbackInfo ci) {
        ShaderPowerState.sync(this);
    }

    @Inject(method = "togglePostEffect", at = @At("HEAD"), cancellable = true)
    private void apoli$lockShaderToggle(CallbackInfo ci) {
        if (ShaderPowerState.locksToggle()) ci.cancel();
    }
}
