package dev.overgrown.apoli.mixin.camera;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.camera.CameraController;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderDispatcherCameraMixin {
    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private boolean apoli$hideCameraAnchor(boolean original, Entity entity, Frustum frustum,
                                           double cameraX, double cameraY, double cameraZ) {
        return original && !CameraController.hides(entity);
    }
}
