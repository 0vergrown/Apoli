package dev.overgrown.apoli.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.ClientTickRates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LevelRendererTickRateMixin {

    @WrapOperation(
        method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apoli$slowedEntityPartial(EntityRenderDispatcher dispatcher, Entity entity,
                                           double x, double y, double z, float yRot, float partialTick,
                                           PoseStack poseStack, MultiBufferSource buffers, int light,
                                           Operation<Void> original) {
        if (!ClientTickRates.idle()) {
            float slot = ClientTickRates.slotPartial(entity, partialTick);
            if (slot != partialTick) {
                if (ClientTickRates.slotDriven(entity)) {
                    float shift = slot - partialTick;
                    x += shift * (entity.getX() - entity.xOld);
                    y += shift * (entity.getY() - entity.yOld);
                    z += shift * (entity.getZ() - entity.zOld);
                    yRot = Mth.lerp(slot, entity.yRotO, entity.getYRot());
                }
                original.call(dispatcher, entity, x, y, z, yRot, slot, poseStack, buffers, light);
                return;
            }
        }
        original.call(dispatcher, entity, x, y, z, yRot, partialTick, poseStack, buffers, light);
    }
}
