package dev.overgrown.apoli.mixin.tick;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.client.ClientTickRates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = LevelRenderer.class, priority = 1100)
@Environment(EnvType.CLIENT)
public abstract class LevelRendererTickRateMixin {

    private static final String RENDER_ENTITY = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V";
    private static final String RENDER = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE", target = RENDER), index = 1)
    private double apoli$slowedX(Entity entity, double x, double y, double z, float yRot, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {
        float shift = apoli$slotShift(entity, partialTick);
        return shift == 0.0F ? x : x + shift * (entity.getX() - entity.xOld);
    }

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE", target = RENDER), index = 2)
    private double apoli$slowedY(Entity entity, double x, double y, double z, float yRot, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {
        float shift = apoli$slotShift(entity, partialTick);
        return shift == 0.0F ? y : y + shift * (entity.getY() - entity.yOld);
    }

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE", target = RENDER), index = 3)
    private double apoli$slowedZ(Entity entity, double x, double y, double z, float yRot, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {
        float shift = apoli$slotShift(entity, partialTick);
        return shift == 0.0F ? z : z + shift * (entity.getZ() - entity.zOld);
    }

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE", target = RENDER), index = 4)
    private float apoli$slowedYaw(Entity entity, double x, double y, double z, float yRot, float partialTick,
                                  PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (apoli$slotShift(entity, partialTick) == 0.0F) return yRot;
        return Mth.lerp(ClientTickRates.slotPartial(entity, partialTick), entity.yRotO, entity.getYRot());
    }

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE", target = RENDER), index = 5)
    private float apoli$slowedPartial(Entity entity, double x, double y, double z, float yRot, float partialTick,
                                      PoseStack poseStack, MultiBufferSource buffers, int light) {
        return ClientTickRates.idle() ? partialTick : ClientTickRates.slotPartial(entity, partialTick);
    }

    @Unique
    private static float apoli$slotShift(Entity entity, float partialTick) {
        if (ClientTickRates.idle()) return 0.0F;
        float slot = ClientTickRates.slotPartial(entity, partialTick);
        if (slot == partialTick || !ClientTickRates.slotDriven(entity)) return 0.0F;
        return slot - partialTick;
    }
}
