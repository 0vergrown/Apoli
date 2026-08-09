package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.PreventEntityRenderHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 800)
@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderDispatcherPreventRenderMixin {

    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private <E extends Entity> boolean apoli$preventEntityCulling(boolean original, E entity, Frustum frustum,
                                                                  double x, double y, double z) {
        return original && !PreventEntityRenderHandler.shouldHide(Minecraft.getInstance().player, entity);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void apoli$preventEntityRender(E entity, double x, double y, double z,
                                                              float yaw, float partialTick, PoseStack pose,
                                                              MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (PreventEntityRenderHandler.shouldHide(Minecraft.getInstance().player, entity)) {
            ci.cancel();
        }
    }
}
