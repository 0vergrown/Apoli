package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.client.particle.AnchoredParticleClient;
import dev.overgrown.apoli.client.render.LevelRenderPass;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LevelRendererPassMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void apoli$beginLevelPass(CallbackInfo ci) {
        AnchoredParticleClient.beginFrame();
        LevelRenderPass.begin();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void apoli$endLevelPass(CallbackInfo ci) {
        LevelRenderPass.end();
    }
}
