package dev.overgrown.apoli.mixin.camera;

import dev.overgrown.apoli.client.camera.CameraController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
@Environment(EnvType.CLIENT)
public abstract class CameraSetupMixin {
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private boolean detached;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void apoli$modifyCamera(BlockGetter level, Entity entity, boolean thirdPerson, boolean mirrored,
                                    float partialTick, CallbackInfo ci) {
        CameraController.Result result = CameraController.compute((Camera) (Object) this, entity, partialTick);
        if (result == null) return;
        this.xRot = result.pitch;
        this.yRot = result.yaw;
        this.rotation.rotationYXZ(-result.yaw * ((float) Math.PI / 180.0F),
            result.pitch * ((float) Math.PI / 180.0F), result.roll * ((float) Math.PI / 180.0F));
        this.forwards.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
        this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
        this.left.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
        this.setPosition(result.x, result.y, result.z);
        if (result.detached) this.detached = true;
    }
}
