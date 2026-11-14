package dev.overgrown.apoli.mixin.modelparts;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.access.BodyEditFollow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ElytraLayer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ElytraLayerFollowBodyMixin {

    @WrapWithCondition(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private boolean apoli$followBody(PoseStack poseStack, float x, float y, float z) {
        if (((RenderLayer<?, ?>) (Object) this).getParentModel() instanceof BodyEditFollow follow) {
            follow.apoli$followBodyEdit(poseStack);
        }
        return true;
    }
}
