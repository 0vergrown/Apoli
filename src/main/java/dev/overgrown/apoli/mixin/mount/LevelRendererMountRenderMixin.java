package dev.overgrown.apoli.mixin.mount;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overgrown.apoli.mount.MountOffsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
        Vec3 attach = vehicle.getPassengerRidingPosition(entity).subtract(vehicle.position());
        Vec3 riderVehicle = entity.getVehicleAttachmentPoint(vehicle);
        Vec3 offset = MountOffsets.resolve(vehicle, entity, partialTick);
        args.set(1, vx + attach.x - riderVehicle.x + offset.x - camX);
        args.set(2, vy + attach.y - riderVehicle.y + offset.y - camY);
        args.set(3, vz + attach.z - riderVehicle.z + offset.z - camZ);
    }
}
