package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.client.PhasingBlindness;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LevelRendererPhasingSkyMixin {

    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", shift = At.Shift.AFTER, ordinal = 0),
        cancellable = true)
    private void apoli$hideSkyWhilePhasing(Matrix4f frustumMatrix, Matrix4f projection, float partialTick, Camera camera, boolean foggy, Runnable fogCallback, CallbackInfo ci) {
        Entity focus = camera.getEntity();
        if (PhasingBlindness.viewDistance(focus) < 0.0F) return;
        if (!PhasingBlindness.inWall(focus)) return;
        ci.cancel();
    }
}
