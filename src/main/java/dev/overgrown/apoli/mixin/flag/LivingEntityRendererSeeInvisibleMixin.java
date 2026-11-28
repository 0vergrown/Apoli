package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.SeeInvisiblePower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// apoli:see_invisible. Vanilla draws an invisible entity the viewer may see at a fixed alpha; this uses the power's
// opacity instead.
@Mixin(LivingEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LivingEntityRendererSeeInvisibleMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String RENDER_TO_BUFFER = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";

    @Unique
    private static final float VANILLA_SEE_THROUGH = 0.15F;

    @Unique
    private float apoli$seeAlpha = -1.0F;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void apoli$pickSeeAlpha(LivingEntity entity, float entityYaw, float partialTick, PoseStack pose,
                                    MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        float opacity = entity.isInvisible() ? SeeInvisiblePower.revealOpacity(Minecraft.getInstance().player, entity) : Float.NaN;
        apoli$seeAlpha = Float.isNaN(opacity) ? -1.0F : 1.0F - opacity;
    }

    @ModifyArg(method = RENDER, at = @At(value = "INVOKE", target = RENDER_TO_BUFFER), index = 7)
    private float apoli$seeInvisibleAlpha(float alpha) {
        float wanted = apoli$seeAlpha;
        apoli$seeAlpha = -1.0F;
        if (wanted < 0.0F || alpha > VANILLA_SEE_THROUGH + 0.0001F) return alpha;
        // Scaled rather than replaced, so a model_color alpha still applies.
        return Math.min(1.0F, alpha / VANILLA_SEE_THROUGH * wanted);
    }
}
