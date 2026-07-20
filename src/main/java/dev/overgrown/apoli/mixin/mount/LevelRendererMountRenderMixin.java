package dev.overgrown.apoli.mixin.mount;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LevelRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LevelRendererMountRenderMixin {

    @ModifyArgs(
        method = "renderEntity",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void apoli$pinRiderToPlayerVehicle(Args args, Entity entity, double camX, double camY, double camZ,
                                               float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        if (!(entity.getVehicle() instanceof Player vehicle)) return;
        double vx = Mth.lerp(partialTick, vehicle.xOld, vehicle.getX());
        double vy = Mth.lerp(partialTick, vehicle.yOld, vehicle.getY());
        double vz = Mth.lerp(partialTick, vehicle.zOld, vehicle.getZ());
        args.set(1, vx - camX);
        args.set(2, vy + vehicle.getBbHeight() + entity.getMyRidingOffset() - camY);
        args.set(3, vz - camZ);
    }
}
