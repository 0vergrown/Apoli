package dev.overgrown.apoli.mixin.projectile;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.entity.ProjectileHitActions;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
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
    @Unique
    private @Nullable Entity apoli$causeHolder;
    @Unique
    private @Nullable ResourceLocation apoli$causePower;
    @Unique
    private int apoli$bounces;
    @Unique
    private boolean apoli$bounced;

    @Override
    public void apoli$setFireConfig(FireProjectilePower.Config config) {
        this.apoli$fireConfig = config;
    }

    @Override
    public void apoli$setMaxRange(double blocks) {
        this.apoli$maxRangeSq = blocks > 0.0 ? blocks * blocks : 0.0;
    }

    @Override
    public void apoli$setFireCause(@Nullable Entity holder, @Nullable ResourceLocation powerId) {
        this.apoli$causeHolder = holder;
        this.apoli$causePower = powerId;
    }

    @Override
    public boolean apoli$bouncedThisHit() {
        return this.apoli$bounced;
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

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$runHitHooks(HitResult result, CallbackInfo ci) {
        this.apoli$bounced = false;
        FireProjectilePower.Config config = apoli$fireConfig;
        if (config == null) return;
        Projectile self = (Projectile) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        FireProjectilePower.Hooks hooks = config.hooks();
        ModifyProjectileDamageHandler.beginProjectileContext(self);
        boolean attributed = dev.overgrown.apoli.attribution.PowerCause.push(
            this.apoli$causeHolder, this.apoli$causePower);
        try {
            if (result.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) result).getEntity();
                if (target != null) {
                    hooks.bientityActionOnHit().ifPresent(a -> a.run(BiEntityCtx.of(self, target, level)));
                    hooks.ownerTargetBientityActionOnHit().ifPresent(a ->
                        a.run(BiEntityCtx.of(self.getOwner(), target, level)));
                }
            } else if (apoli$tryBounce(self, level, config, result)) {
                this.apoli$bounced = true;
            } else {
                apoli$runMissHooks(self, level, hooks, config.params().blockActionCancelsMissAction(), result);
            }
        } finally {
            if (attributed) dev.overgrown.apoli.attribution.PowerCause.pop();
            ModifyProjectileDamageHandler.endProjectileContext();
        }
        if (this.apoli$bounced) ci.cancel();
    }

    @Unique
    private boolean apoli$tryBounce(Projectile self, Level level, FireProjectilePower.Config config,
                                    HitResult result) {
        FireProjectilePower.Params params = config.params();
        if (!params.reflective()) return false;
        if (!(result instanceof BlockHitResult blockHit)) return false;
        int limit = params.maxBounces();
        if (limit >= 0 && this.apoli$bounces >= limit) return false;

        FireProjectilePower.Hooks hooks = config.hooks();
        if (hooks.blockActionOnHit().isPresent()) {
            BlockPos pos = blockHit.getBlockPos();
            BlockCtx blockCtx = new BlockCtx(pos.immutable(), level.getBlockState(pos), level,
                self.getOwner() != null ? self.getOwner() : self, blockHit.getLocation());
            if (hooks.blockCondition().isEmpty() || hooks.blockCondition().get().test(blockCtx)) {
                hooks.blockActionOnHit().get().run(blockCtx);
            }
        }

        Direction face = blockHit.getDirection();
        Vec3 velocity = self.getDeltaMovement();
        double dot = velocity.x * face.getStepX() + velocity.y * face.getStepY() + velocity.z * face.getStepZ();
        Vec3 reflected = new Vec3(
            velocity.x - 2.0 * dot * face.getStepX(),
            velocity.y - 2.0 * dot * face.getStepY(),
            velocity.z - 2.0 * dot * face.getStepZ()).scale(params.bounceSpeed());
        if (reflected.lengthSqr() < 1.0E-6) return false;

        this.apoli$bounces++;
        Vec3 landing = blockHit.getLocation();
        double clearance = Math.max(self.getBbWidth(), self.getBbHeight()) * 0.5 + 0.01;
        self.setPos(landing.x + face.getStepX() * clearance,
            landing.y + face.getStepY() * clearance,
            landing.z + face.getStepZ() * clearance);
        self.setDeltaMovement(reflected);
        double horizontal = reflected.horizontalDistance();
        self.setYRot((float) (Mth.atan2(reflected.x, reflected.z) * (180.0 / Math.PI)));
        self.setXRot((float) (Mth.atan2(reflected.y, horizontal) * (180.0 / Math.PI)));
        self.yRotO = self.getYRot();
        self.xRotO = self.getXRot();
        self.hasImpulse = true;
        this.apoli$missFired = false;

        hooks.bientityActionOnBounce().ifPresent(a -> a.run(BiEntityCtx.of(self.getOwner(), self, level)));
        return true;
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
