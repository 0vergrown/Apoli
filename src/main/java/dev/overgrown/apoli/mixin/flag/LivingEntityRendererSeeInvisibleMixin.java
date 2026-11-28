package dev.overgrown.apoli.mixin.flag;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.power.builtin.SeeInvisiblePower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// apoli:see_invisible. Vanilla draws an invisible entity the viewer may see at a fixed alpha; this uses the power's
// opacity instead.
@Mixin(LivingEntityRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRendererSeeInvisibleMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String RENDER_TO_BUFFER = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";

    // Vanilla's alpha for an invisible entity the viewer may see (0x26 of 0xFF).
    @Unique
    private static final int VANILLA_SEE_THROUGH = 0x26;

    @Unique
    private float apoli$seeAlpha = -1.0F;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void apoli$pickSeeAlpha(LivingEntity entity, float entityYaw, float partialTick, PoseStack pose,
                                    MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        float opacity = entity.isInvisible() ? SeeInvisiblePower.revealOpacity(Minecraft.getInstance().player, entity) : Float.NaN;
        apoli$seeAlpha = Float.isNaN(opacity) ? -1.0F : 1.0F - opacity;
    }

    @ModifyArg(method = RENDER, at = @At(value = "INVOKE", target = RENDER_TO_BUFFER), index = 4)
    private int apoli$seeInvisibleAlpha(int color) {
        float wanted = apoli$seeAlpha;
        apoli$seeAlpha = -1.0F;
        int alpha = color >>> 24;
        if (wanted < 0.0F || alpha > VANILLA_SEE_THROUGH) return color;
        // Scaled rather than replaced, so a model_color alpha still applies.
        int scaled = Math.min(255, Math.round(alpha * 255.0F / VANILLA_SEE_THROUGH * wanted));
        return scaled << 24 | (color & 0xFFFFFF);
    }
}
