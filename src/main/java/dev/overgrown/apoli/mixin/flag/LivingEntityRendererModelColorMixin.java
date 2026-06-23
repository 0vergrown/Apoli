package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererModelColorMixin {

    @ModifyArgs(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void apoli$applyModelColor(Args args,
                                       LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        float[] color = ModelColorPower.colorFor(entity);
        if (color == ModelColorPower.IDENTITY) return;
        args.set(4, (float) args.get(4) * color[0]);
        args.set(5, (float) args.get(5) * color[1]);
        args.set(6, (float) args.get(6) * color[2]);
        args.set(7, (float) args.get(7) * color[3]);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void apoli$forceTranslucent(LivingEntity entity, boolean isVisible, boolean isInvisibleToPlayer,
                                        boolean useTranslucent, CallbackInfoReturnable<RenderType> cir) {
        if (!isVisible) return;
        if (isInvisibleToPlayer) return;
        float[] color = ModelColorPower.colorFor(entity);
        if (color == ModelColorPower.IDENTITY) return;
        if (color[3] >= 0.999f) return;
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, ?> self = (LivingEntityRenderer<LivingEntity, ?>) (Object) this;
        ResourceLocation tex = self.getTextureLocation(entity);
        cir.setReturnValue(RenderType.itemEntityTranslucentCull(tex));
    }
}
