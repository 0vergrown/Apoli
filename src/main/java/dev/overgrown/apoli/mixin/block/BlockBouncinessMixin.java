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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockBouncinessMixin {
    @Inject(method = "updateEntityAfterFallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void apoli$updateEntityAfterFallOn(BlockGetter blockGetter, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living) || !ModifyBouncinessHandler.has(living)) return;
        Vec3 oldDelta = entity.getDeltaMovement();
        if (oldDelta.y >= 0.0) return;
        if (living.hasLandedInLiquid()) return;
        BlockPos pos = entity.getOnPos();
        BlockCtx block = new BlockCtx(pos, blockGetter.getBlockState(pos), entity.level());
        if (entity.isSuppressingBounce() && ModifyBouncinessHandler.preventable(living, block)) return;

        double delta = ModifyBouncinessHandler.modify(living, oldDelta.y * -1, block);
        if (delta <= 0.125) return;

        PowerLookup.forEach(entity, Apoli.id("modify_bounciness"), ModifyBouncinessPower.Config.class, cfg -> {
            if (cfg.blockCondition().isEmpty() || cfg.blockCondition().get().test(block)) {
                cfg.blockAction().ifPresent(action -> action.run(block));
                cfg.entityAction().ifPresent(action -> action.run(EntityCtx.of(entity, entity.level())));
            }
        });

        entity.setDeltaMovement(oldDelta.x, delta, oldDelta.z);
        ci.cancel();
    }

    @Inject(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z"), cancellable = true)
    public void apoli$fallOn(Level level, BlockState blockState, BlockPos blockPos, Entity entity, float f, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living) || !ModifyBouncinessHandler.has(living)) return;
        if (ModifyBouncinessHandler.preventsFallDamage(living, new BlockCtx(blockPos, blockState, level))) {
            ci.cancel();
        }
    }
}
