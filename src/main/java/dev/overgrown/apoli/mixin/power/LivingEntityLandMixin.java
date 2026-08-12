package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ActionOnLandPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLandMixin {

    @Inject(
        method = "checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At("HEAD"))
    private void apoli$actionOnLand(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!onGround || self.fallDistance <= 0.0F || self.level().isClientSide()) return;
        ActionOnLandPower.onLand(self);
    }
}
