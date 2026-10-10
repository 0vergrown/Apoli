package dev.overgrown.apoli.mixin.firstperson;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overgrown.apoli.client.render.GhostArmState;
import dev.overgrown.apoli.client.render.ModelColorState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class PlayerRendererGhostArmMixin {

    @WrapOperation(method = "renderHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType apoli$translucentArmType(ResourceLocation texture, Operation<RenderType> original,
                                                @Local(argsOnly = true, ordinal = 0) ModelPart arm) {
        if (GhostArmState.isActive()) return RenderType.entityTranslucent(texture);
        if (ModelColorState.alphaFor(arm) < 0.999F) return RenderType.entityTranslucentCull(texture);
        return original.call(texture);
    }

    @WrapOperation(method = "renderHand",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void apoli$ghostArmColor(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                     Operation<Void> original) {
        if (!GhostArmState.isActive()) {
            original.call(part, pose, consumer, light, overlay);
            return;
        }
        part.render(pose, consumer, light, overlay, 1.0F, 1.0F, 1.0F, GhostArmState.alpha());
    }
}
