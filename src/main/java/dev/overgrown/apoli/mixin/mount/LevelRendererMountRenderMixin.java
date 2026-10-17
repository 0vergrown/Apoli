package dev.overgrown.apoli.mixin.mount;

import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.mount.MountOffsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LevelRendererMountRenderMixin {

    private static final String RENDER = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @ModifyArg(method = "renderEntity", at = @At(value = "INVOKE", target = RENDER), index = 1)
    private double apoli$pinRiderX(double x, @Local(argsOnly = true) Entity entity,
                                   @Local(argsOnly = true, ordinal = 0) double camX,
                                   @Local(argsOnly = true) float partialTick) {
        if (!(entity.getVehicle() instanceof Player vehicle)) return x;
        return MountOffsets.riderRenderPosition(entity, vehicle, partialTick).x - camX;
    }

    @ModifyArg(method = "renderEntity", at = @At(value = "INVOKE", target = RENDER), index = 2)
    private double apoli$pinRiderY(double y, @Local(argsOnly = true) Entity entity,
                                   @Local(argsOnly = true, ordinal = 1) double camY,
                                   @Local(argsOnly = true) float partialTick) {
        if (!(entity.getVehicle() instanceof Player vehicle)) return y;
        return MountOffsets.riderRenderPosition(entity, vehicle, partialTick).y - camY;
    }

    @ModifyArg(method = "renderEntity", at = @At(value = "INVOKE", target = RENDER), index = 3)
    private double apoli$pinRiderZ(double z, @Local(argsOnly = true) Entity entity,
                                   @Local(argsOnly = true, ordinal = 2) double camZ,
                                   @Local(argsOnly = true) float partialTick) {
        if (!(entity.getVehicle() instanceof Player vehicle)) return z;
        return MountOffsets.riderRenderPosition(entity, vehicle, partialTick).z - camZ;
    }
}
