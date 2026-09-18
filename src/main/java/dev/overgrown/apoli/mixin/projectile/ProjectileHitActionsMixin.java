package dev.overgrown.apoli.mixin.projectile;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.entity.ProjectileHitActions;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileHitActionsMixin implements ProjectileHitActions {

    @Unique
    private @Nullable FireProjectilePower.Config apoli$fireConfig;
    @Unique
    private boolean apoli$missFired;
    @Unique
    private double apoli$maxRangeSq;
    @Unique
    private boolean apoli$originSet;
    @Unique
    private double apoli$originX;
    @Unique
    private double apoli$originY;
    @Unique
    private double apoli$originZ;

    @Override
    public void apoli$setFireConfig(FireProjectilePower.Config config) {
        this.apoli$fireConfig = config;
    }

    @Override
    public void apoli$setMaxRange(double blocks) {
        this.apoli$maxRangeSq = blocks > 0.0 ? blocks * blocks : 0.0;
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void apoli$enforceMaxRange(CallbackInfo ci) {
        if (this.apoli$maxRangeSq <= 0.0) return;
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide()) return;
        if (!this.apoli$originSet) {
            this.apoli$originSet = true;
            this.apoli$originX = self.getX();
            this.apoli$originY = self.getY();
            this.apoli$originZ = self.getZ();
            return;
        }
        if (self.distanceToSqr(this.apoli$originX, this.apoli$originY, this.apoli$originZ) > this.apoli$maxRangeSq) {
            self.discard();
        }
    }

    @Inject(method = "canHitEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void apoli$filterHitTargets(Entity target, CallbackInfoReturnable<Boolean> cir) {
        FireProjectilePower.Config config = apoli$fireConfig;
        if (config == null) return;
        FireProjectilePower.Hooks hooks = config.hooks();
        if (hooks.bientityCondition().isEmpty() && hooks.ownerBientityCondition().isEmpty()) return;

        Projectile self = (Projectile) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        if (hooks.bientityCondition().isPresent()
            && !hooks.bientityCondition().get().test(BiEntityCtx.of(self, target, level))) {
            cir.setReturnValue(false);
            return;
        }
        if (hooks.ownerBientityCondition().isPresent()
            && !hooks.ownerBientityCondition().get().test(BiEntityCtx.of(self.getOwner(), target, level))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"))
    private void apoli$runHitHooks(HitResult result, CallbackInfo ci) {
        FireProjectilePower.Config config = apoli$fireConfig;
        if (config == null) return;
        Projectile self = (Projectile) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        FireProjectilePower.Hooks hooks = config.hooks();
        ModifyProjectileDamageHandler.beginProjectileContext(self);
        try {
            if (result.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) result).getEntity();
                if (target != null) {
                    hooks.bientityActionOnHit().ifPresent(a -> a.run(BiEntityCtx.of(self, target, level)));
                    hooks.ownerTargetBientityActionOnHit().ifPresent(a ->
                        a.run(BiEntityCtx.of(self.getOwner(), target, level)));
                }
            } else {
                apoli$runMissHooks(self, level, hooks, config.params().blockActionCancelsMissAction(), result);
            }
        } finally {
            ModifyProjectileDamageHandler.endProjectileContext();
        }
    }

    @Unique
    private void apoli$runMissHooks(Projectile self, Level level, FireProjectilePower.Hooks hooks,
                                    boolean blockActionCancelsMiss, HitResult result) {
        boolean blockActionRan = false;
        if (hooks.blockActionOnHit().isPresent() && result instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockCtx blockCtx = new BlockCtx(pos.immutable(), level.getBlockState(pos), level,
                self.getOwner() != null ? self.getOwner() : self, blockHit.getLocation());
            if (hooks.blockCondition().isEmpty() || hooks.blockCondition().get().test(blockCtx)) {
                hooks.blockActionOnHit().get().run(blockCtx);
                blockActionRan = true;
            }
        }
        if (apoli$missFired || (blockActionRan && blockActionCancelsMiss)) return;
        apoli$missFired = true;
        hooks.bientityActionOnMiss().ifPresent(a -> a.run(BiEntityCtx.of(self.getOwner(), self, level)));
    }
}
