package dev.overgrown.apoli.mixin.block;


import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ModifyBouncinessPower;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.builtin.ModifyBouncinessHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockBouncinessMixin {
    @Inject(method = "updateEntityAfterFallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void apoli$updateEntityAfterFallOn(BlockGetter blockGetter, Entity entity, CallbackInfo ci) {
        var pos = entity.getOnPos();
        var block = new BlockCtx(pos, blockGetter.getBlockState(pos), entity.level());
        if (entity instanceof LivingEntity && (!entity.isSuppressingBounce() || !ModifyBouncinessHandler.preventable((LivingEntity) entity, block)) && !entity.level().isFluidAtPosition(entity.blockPosition(), (state -> !state.isEmpty()))) {
            var old_delta = entity.getDeltaMovement();
            var delta = ModifyBouncinessHandler.modify((LivingEntity) entity, old_delta.y * -1, block);

            if (delta > 0.125 && old_delta.y < 0) {
                PowerLookup.forEach(entity, Apoli.id("modify_bounciness"), ModifyBouncinessPower.Config.class, cfg -> {
                    if (cfg.blockCondition().isEmpty() || cfg.blockCondition().get().test(block)) {
                        cfg.blockAction().ifPresent(action -> action.run(block));
                        cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(entity, entity.level())));
                    }
                });

                entity.setDeltaMovement(old_delta.x, delta, old_delta.z);
                ci.cancel();
            }
        }
    }

    @Inject(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z"), cancellable = true)
    public void apoli$fallOn(Level level, BlockState blockState, BlockPos blockPos, Entity entity, float f, CallbackInfo ci) {
        if (entity instanceof LivingEntity) {
            var block = new BlockCtx(blockPos, blockState, level);

            if (!ModifyBouncinessHandler.damage((LivingEntity) entity, block)) {
                ci.cancel();
            }
        }
    }
}
