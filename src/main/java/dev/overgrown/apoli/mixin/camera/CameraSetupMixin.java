package dev.overgrown.apoli.mixin.camera;

import dev.overgrown.apoli.client.camera.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
@OnlyIn(Dist.CLIENT)
public abstract class CameraSetupMixin {
    @Shadow private boolean detached;
    @Shadow private float roll;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch, float roll);

    @Inject(method = "setup", at = @At("TAIL"))
    private void apoli$modifyCamera(BlockGetter level, Entity entity, boolean thirdPerson, boolean mirrored,
                                    float partialTick, CallbackInfo ci) {
        CameraController.Result result = CameraController.compute((Camera) (Object) this, entity, partialTick);
        if (result == null) return;
        this.setRotation(result.yaw, result.pitch, result.ridesVanilla ? this.roll + result.roll : result.roll);
        this.setPosition(result.x, result.y, result.z);
        if (result.detached) this.detached = true;
    }
}
