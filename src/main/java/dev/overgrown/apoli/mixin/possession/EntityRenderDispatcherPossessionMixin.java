package dev.overgrown.apoli.mixin.possession;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.possession.ClientPossession;
import dev.overgrown.apoli.entity.possession.Possession;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 700)
@Environment(EnvType.CLIENT)
public abstract class EntityRenderDispatcherPossessionMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void apoli$renderHostInstead(E entity, double x, double y, double z, float yaw,
                                                            float partialTick, PoseStack pose, MultiBufferSource buffers,
                                                            int light, CallbackInfo ci) {
        if (!ClientPossession.active()) return;
        Entity host = Possession.hostOf(entity);
        if (host == null) return;
        ci.cancel();
        if (entity != Minecraft.getInstance().getCameraEntity()) return;
        float hostYaw = Mth.lerp(partialTick, host.yRotO, host.getYRot());
        ((EntityRenderDispatcher) (Object) this).render(host, x, y, z, hostYaw, partialTick, pose, buffers, light);
    }

    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private <E extends Entity> boolean apoli$hideCameraHost(boolean original, E entity, Frustum frustum,
                                                            double x, double y, double z) {
        if (!original || !ClientPossession.active()) return original;
        return !ClientPossession.isCameraHost(entity);
    }
}
