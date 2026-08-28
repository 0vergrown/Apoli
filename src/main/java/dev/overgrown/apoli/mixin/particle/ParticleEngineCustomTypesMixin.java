package dev.overgrown.apoli.mixin.particle;

import dev.overgrown.apoli.client.particle.ApoliParticleRenderTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
@Environment(EnvType.CLIENT)
public abstract class ParticleEngineCustomTypesMixin {

    @Shadow
    @Final
    private Map<ParticleRenderType, Queue<Particle>> particles;

    @Shadow
    @Final
    private TextureManager textureManager;

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;turnOffLightLayer()V"))
    private void apoli$renderCustomParticles(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        ApoliParticleRenderTypes.render(this.particles, this.textureManager, camera, partialTick);
    }
}
