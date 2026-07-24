package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.client.render.ModelColorState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelPart.class)
@Environment(EnvType.CLIENT)
public abstract class ModelPartModelColorMixin {

    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";

    @ModifyVariable(method = RENDER, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int apoli$whitenPart(int overlay) {
        if (!ModelColorState.isActive()) return overlay;
        float[] c = ModelColorState.colorFor((ModelPart) (Object) this);
        if (c == null || c[4] < 1f) return overlay;
        return OverlayTexture.pack(15, overlay >> 16 & 0xFFFF);
    }

    @ModifyVariable(method = RENDER, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float apoli$tintRed(float value) {
        return apoli$tint(value, 0);
    }

    @ModifyVariable(method = RENDER, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float apoli$tintGreen(float value) {
        return apoli$tint(value, 1);
    }

    @ModifyVariable(method = RENDER, at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float apoli$tintBlue(float value) {
        return apoli$tint(value, 2);
    }

    @ModifyVariable(method = RENDER, at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float apoli$tintAlpha(float value) {
        return apoli$tint(value, 3);
    }

    @Unique
    private float apoli$tint(float value, int channel) {
        if (!ModelColorState.isActive()) return value;
        float[] c = ModelColorState.colorFor((ModelPart) (Object) this);
        return c == null ? value : value * c[channel];
    }
}
