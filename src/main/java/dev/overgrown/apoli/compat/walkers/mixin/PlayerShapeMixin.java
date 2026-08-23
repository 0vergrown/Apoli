package dev.overgrown.apoli.compat.walkers.mixin;

import dev.overgrown.apoli.compat.walkers.power.ShapePowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "tocraft.walkers.api.PlayerShape", remap = false)
public abstract class PlayerShapeMixin {

    @Inject(method = "updateShapes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void apoli$preventShapeChange(ServerPlayer player, LivingEntity shape,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (ShapePowers.preventShapeChange(player, shape)) cir.setReturnValue(false);
    }

    @Inject(method = "updateShapes", at = @At("RETURN"), remap = false)
    private static void apoli$actionOnShapeChange(ServerPlayer player, LivingEntity shape,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) ShapePowers.fireShapeChange(player, shape);
    }
}
