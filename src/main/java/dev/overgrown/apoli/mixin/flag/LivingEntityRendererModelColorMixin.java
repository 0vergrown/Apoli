package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.render.ModelColorState;
import dev.overgrown.apoli.client.render.ModelPartLookup;
import dev.overgrown.apoli.power.builtin.ModelColorPower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Map;

@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRendererModelColorMixin {

    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void apoli$setupPartColors(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack pose, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ModelColorState.clear();
        if (!ModelColorPower.hasPartColors(entity)) return;
        EntityModel<?> model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
        if (!(model instanceof HumanoidModel<?> humanoid)) return;
        Map<String, float[]> parts = ModelColorPower.partColorsFor(entity);
        if (parts == null) return;
        ModelColorState.set(ModelPartLookup.buildColorMap(humanoid, parts));
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void apoli$clearPartColors(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack pose, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ModelColorState.clear();
    }

    @ModifyArgs(
        method = RENDER,
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void apoli$applyModelColor(Args args,
                                       LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        float[] color = ModelColorPower.colorFor(entity);
        if (color == ModelColorPower.IDENTITY) return;
        int packedColor = args.<Integer>get(4);
        int a = Math.min(255, Math.round(FastColor.ARGB32.alpha(packedColor) * color[3]));
        int r = Math.min(255, Math.round(FastColor.ARGB32.red(packedColor) * color[0]));
        int g = Math.min(255, Math.round(FastColor.ARGB32.green(packedColor) * color[1]));
        int b = Math.min(255, Math.round(FastColor.ARGB32.blue(packedColor) * color[2]));
        args.set(4, FastColor.ARGB32.color(a, r, g, b));
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void apoli$forceTranslucent(LivingEntity entity, boolean isVisible, boolean isInvisibleToPlayer,
                                        boolean useTranslucent, CallbackInfoReturnable<RenderType> cir) {
        if (!isVisible) return;
        if (isInvisibleToPlayer) return;
        if (ModelColorPower.minAlpha(entity) >= 0.999f) return;
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, ?> self = (LivingEntityRenderer<LivingEntity, ?>) (Object) this;
        ResourceLocation tex = self.getTextureLocation(entity);
        cir.setReturnValue(RenderType.itemEntityTranslucentCull(tex));
    }
}
