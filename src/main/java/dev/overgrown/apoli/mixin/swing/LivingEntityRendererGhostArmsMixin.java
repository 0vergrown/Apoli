package dev.overgrown.apoli.mixin.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.GhostArmTrails;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRendererGhostArmsMixin {

    @Shadow
    protected abstract boolean isBodyVisible(LivingEntity entity);

    @SuppressWarnings("unchecked")
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void apoli$thirdPersonGhostArms(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                            MultiBufferSource buffers, int light, CallbackInfo ci) {
        LivingEntityRenderer<?, ?> self = (LivingEntityRenderer<?, ?>) (Object) this;
        if (!(self.getModel() instanceof HumanoidModel<?> model) || model.attackTime <= 0.0F) return;
        if (entity.isSpectator() || !this.isBodyVisible(entity)) return;
        GhostArmTrails.render(model, entity, poseStack, buffers,
            ((EntityRenderer<LivingEntity>) (Object) this).getTextureLocation(entity), light);
    }
}
