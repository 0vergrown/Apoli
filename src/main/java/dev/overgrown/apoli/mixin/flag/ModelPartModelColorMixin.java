package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.client.render.ModelColorState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelPart.class)
@OnlyIn(Dist.CLIENT)
public abstract class ModelPartModelColorMixin {

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int apoli$whitenPart(int overlay) {
        if (!ModelColorState.isActive()) return overlay;
        float[] c = ModelColorState.colorFor((ModelPart) (Object) this);
        if (c == null || c[4] < 1f) return overlay;
        return OverlayTexture.pack(15, overlay >> 16 & 0xFFFF);
    }

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int apoli$tintPart(int color) {
        if (!ModelColorState.isActive()) return color;
        float[] c = ModelColorState.colorFor((ModelPart) (Object) this);
        if (c == null) return color;
        int a = Math.min(255, Math.round(FastColor.ARGB32.alpha(color) * c[3]));
        int r = Math.min(255, Math.round(FastColor.ARGB32.red(color) * c[0]));
        int g = Math.min(255, Math.round(FastColor.ARGB32.green(color) * c[1]));
        int b = Math.min(255, Math.round(FastColor.ARGB32.blue(color) * c[2]));
        return FastColor.ARGB32.color(a, r, g, b);
    }
}
