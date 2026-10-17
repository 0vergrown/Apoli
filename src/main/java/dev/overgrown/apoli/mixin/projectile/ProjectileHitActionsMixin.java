package dev.overgrown.apoli.mixin.projectile;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.entity.CustomProjectileEntity;
import dev.overgrown.apoli.entity.ProjectileHitActions;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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

import java.util.Optional;

@Mixin(Projectile.class)
public abstract class ProjectileHitActionsMixin implements ProjectileHitActions {

    @Unique
    private static final double APOLI$ENTITY_HIT_MARGIN = 0.3;
    @Unique
    private static final double APOLI$FACE_EPSILON = 1.0E-4;
    @Unique
    private static final double APOLI$STAND_OFF = 0.05;
    @Unique
    private static final Vec3 APOLI$WEST = new Vec3(-1.0, 0.0, 0.0);
    @Unique
    private static final Vec3 APOLI$EAST = new Vec3(1.0, 0.0, 0.0);
    @Unique
    private static final Vec3 APOLI$NORTH = new Vec3(0.0, 0.0, -1.0);
    @Unique
    private static final Vec3 APOLI$SOUTH = new Vec3(0.0, 0.0, 1.0);
    @Unique
    private static final Vec3 APOLI$DOWN = new Vec3(0.0, -1.0, 0.0);
    @Unique
    private static final Vec3 APOLI$UP = new Vec3(0.0, 1.0, 0.0);

    @Unique
    private @Nullable FireProjectilePower.Config apoli$fireConfig;
    @Unique
    private boolean apoli$missFired;
    @Unique
    private double apoli$maxRange;
    @Unique
    private double apoli$travelled;
    @Unique
    private int apoli$lifetime;
    @Unique
    private int apoli$age;
    @Unique
    private boolean apoli$lastSet;
    @Unique
    private double apoli$lastX;
    @Unique
    private double apoli$lastY;
    @Unique
    private double apoli$lastZ;
    @Unique
    private @Nullable Entity apoli$causeHolder;
    @Unique
    private @Nullable ResourceLocation apoli$causePower;
    @Unique
    private int apoli$bounces;
    @Unique
    private boolean apoli$keepsFlying;
    @Unique
    private int apoli$flightTicks;
    @Unique
    private int apoli$lastHit = -1;
    @Unique
    private int apoli$pierced;
    @Unique
    private @Nullable IntArrayList apoli$passed;
    @Unique
    private int apoli$bouncedOff = -1;

    @Override
    public void apoli$setFireConfig(FireProjectilePower.Config config) {
        this.apoli$fireConfig = config;
    }

    @Override
    public void apoli$setMaxRange(double blocks) {
        this.apoli$maxRange = blocks > 0.0 ? blocks : 0.0;
    }

    @Override
    public void apoli$setLifetime(int ticks) {
        this.apoli$lifetime = Math.max(0, ticks);
    }

    @Override
    public void apoli$setFireCause(@Nullable Entity holder, @Nullable ResourceLocation powerId) {
        this.apoli$causeHolder = holder;
        this.apoli$causePower = powerId;
    }

    @Override
    public boolean apoli$keepsFlying() {
        return this.apoli$keepsFlying;
    }

    @Override
    public void apoli$caught() {
        FireProjectilePower.Config config = this.apoli$fireConfig;
        if (config == null) return;
        FireProjectilePower.Return ret = config.returning().orElse(null);
        if (ret != null) apoli$runOwnerAction((Projectile) (Object) this, ret.bientityActionOnCatch());
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void apoli$tickFired(CallbackInfo ci) {
        FireProjectilePower.Config config = this.apoli$fireConfig;
        if (config == null) return;
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide() || self.isRemoved()) return;
        if (this.apoli$lifetime > 0 && ++this.apoli$age > this.apoli$lifetime) {
            apoli$runOwnerAction(self, config.hooks().bientityActionOnExpire());
            self.discard();
            return;
        }
        CustomProjectileEntity custom = self instanceof CustomProjectileEntity projectile ? projectile : null;
        if (custom != null && custom.isReturning()) return;
        FireProjectilePower.Return ret = custom == null ? null : config.returning().orElse(null);
        if (ret != null && ret.after() > 0 && ++this.apoli$flightTicks >= ret.after()
            && apoli$mayTurn(self, ret) && apoli$startReturn(self, custom, ret, null, false)) {
            return;
        }
        if (this.apoli$maxRange <= 0.0) return;
        if (!this.apoli$lastSet) {
            this.apoli$lastSet = true;
            this.apoli$lastX = self.getX();
            this.apoli$lastY = self.getY();
            this.apoli$lastZ = self.getZ();
            return;
        }
        double dx = self.getX() - this.apoli$lastX;
        double dy = self.getY() - this.apoli$lastY;
        double dz = self.getZ() - this.apoli$lastZ;
        this.apoli$lastX = self.getX();
        this.apoli$lastY = self.getY();
        this.apoli$lastZ = self.getZ();
        this.apoli$travelled += Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (this.apoli$travelled <= this.apoli$maxRange) return;
        if (ret != null) {
            if (!apoli$mayTurn(self, ret)) return;
            if (apoli$startReturn(self, custom, ret, null, false)) return;
        }
        apoli$runOwnerAction(self, config.hooks().bientityActionOnExpire());
        self.discard();
    }

    @Unique
    private static boolean apoli$mayTurn(Projectile self, FireProjectilePower.Return ret) {
        if (ret.condition().isEmpty()) return true;
        Entity owner = self.getOwner();
        return owner == null || ret.condition().get().test(BiEntityCtx.of(owner, self, self.level()));
    }

    @ModifyReturnValue(method = "canHitEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private boolean apoli$filterHitTargets(boolean original, Entity target) {
        FireProjectilePower.Config config = apoli$fireConfig;
        if (!original || config == null) return original;
        Projectile self = (Projectile) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return true;
        int targetId = target.getId();
        if (targetId == this.apoli$bouncedOff || (this.apoli$passed != null && this.apoli$passed.contains(targetId))) {
            return false;
        }

        if (self instanceof CustomProjectileEntity custom && custom.isReturning()) {
            FireProjectilePower.Return ret = config.returning().orElse(null);
            if (ret == null || !ret.hitWhileReturning() || target == self.getOwner()
                || target.getId() == this.apoli$lastHit) {
                return false;
            }
        }

        FireProjectilePower.Hooks hooks = config.hooks();
        if (hooks.bientityCondition().isPresent()
            && !hooks.bientityCondition().get().test(BiEntityCtx.of(self, target, level))) {
            return false;
        }
        return hooks.ownerBientityCondition().isEmpty()
            || hooks.ownerBientityCondition().get().test(BiEntityCtx.of(self.getOwner(), target, level));
    }

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void apoli$runHitHooks(HitResult result, CallbackInfo ci) {
        this.apoli$keepsFlying = false;
        FireProjectilePower.Config config = apoli$fireConfig;
        if (config == null) return;
        Projectile self = (Projectile) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        FireProjectilePower.Hooks hooks = config.hooks();
        boolean entityHit = result.getType() == HitResult.Type.ENTITY;
        ModifyProjectileDamageHandler.beginProjectileContext(self);
        boolean attributed = dev.overgrown.apoli.attribution.PowerCause.push(
            this.apoli$causeHolder, this.apoli$causePower);
        try {
            if (entityHit) {
                Entity target = ((EntityHitResult) result).getEntity();
                if (target != null) {
                    this.apoli$lastHit = target.getId();
                    hooks.bientityActionOnHit().ifPresent(a -> a.run(BiEntityCtx.of(self, target, level)));
                    hooks.ownerTargetBientityActionOnHit().ifPresent(a ->
                        a.run(BiEntityCtx.of(self.getOwner(), target, level)));
                    if (!self.isRemoved() && !apoli$returning(self)
                        && (apoli$bounceOffEntity(self, level, config, target) || apoli$pierce(self, config, target))) {
                        this.apoli$keepsFlying = true;
                    }
                }
            } else if (apoli$tryBounce(self, level, config, result)) {
                this.apoli$keepsFlying = true;
            } else {
                apoli$runMissHooks(self, level, hooks, config.params().blockActionCancelsMissAction(), result);
            }
        } finally {
            if (attributed) dev.overgrown.apoli.attribution.PowerCause.pop();
            ModifyProjectileDamageHandler.endProjectileContext();
        }
        if (this.apoli$keepsFlying) {
            ci.cancel();
            return;
        }
        if (self instanceof CustomProjectileEntity custom && !custom.isReturning()) {
            FireProjectilePower.Return ret = config.returning().orElse(null);
            if (ret == null) return;
            if (entityHit ? ret.onHitEntity() : ret.onHitBlock() && result instanceof BlockHitResult) {
                apoli$startReturn(self, custom, ret, entityHit ? null : (BlockHitResult) result, entityHit);
            }
        }
    }

    @Unique
    private boolean apoli$startReturn(Projectile self, CustomProjectileEntity custom, FireProjectilePower.Return ret,
                                      @Nullable BlockHitResult blockHit, boolean afterEntityHit) {
        Entity owner = self.getOwner();
        if (owner == null) return false;
        float pull = (float) ret.speed().eval(owner);
        if (!(pull > 0.0F)) return false;
        if (blockHit != null) {
            apoli$standOff(self, blockHit);
            self.setDeltaMovement(Vec3.ZERO);
        }
        custom.startReturning(pull);
        self.hasImpulse = true;
        apoli$newLeg();
        if (!afterEntityHit) this.apoli$lastHit = -1;
        apoli$runOwnerAction(self, ret.bientityActionOnReturn());
        return true;
    }

    @Unique
    private void apoli$newLeg() {
        if (this.apoli$passed != null) this.apoli$passed.clear();
        this.apoli$bouncedOff = -1;
    }

    @Unique
    private static boolean apoli$returning(Projectile self) {
        return self instanceof CustomProjectileEntity custom && custom.isReturning();
    }

    @Unique
    private boolean apoli$pierce(Projectile self, FireProjectilePower.Config config, Entity target) {
        int limit = config.pierce().evalInt(self.getOwner());
        if (limit == 0 || (limit > 0 && this.apoli$pierced >= limit)) return false;
        this.apoli$pierced++;
        if (this.apoli$passed == null) this.apoli$passed = new IntArrayList(4);
        this.apoli$passed.add(target.getId());
        return true;
    }

    @Unique
    private boolean apoli$bounceOffEntity(Projectile self, Level level, FireProjectilePower.Config config,
                                          Entity target) {
        FireProjectilePower.Reflect reflect = config.reflective().orElse(null);
        if (reflect == null || !reflect.entities()) return false;
        Entity owner = self.getOwner();
        int limit = reflect.maxBounces().evalInt(owner);
        if (limit >= 0 && this.apoli$bounces >= limit) return false;

        Vec3 velocity = self.getDeltaMovement();
        Vec3 start = self.position();
        AABB box = target.getBoundingBox().inflate(APOLI$ENTITY_HIT_MARGIN);
        Vec3 entry = box.clip(start, start.add(velocity)).orElse(null);
        Vec3 normal = entry == null ? null : apoli$faceNormal(box, entry);
        if (normal == null) {
            Vec3 center = box.getCenter();
            normal = new Vec3(start.x - center.x, 0.0, start.z - center.z);
            normal = normal.lengthSqr() < 1.0E-8 ? velocity.scale(-1.0) : normal;
            if (normal.lengthSqr() < 1.0E-12) return false;
            normal = normal.normalize();
        }
        double dot = velocity.dot(normal);
        Vec3 reflected = (dot < 0.0 ? velocity.subtract(normal.scale(2.0 * dot)) : velocity)
            .scale(reflect.speed().eval(owner));
        if (reflected.lengthSqr() < 1.0E-6) return false;

        this.apoli$bounces++;
        this.apoli$bouncedOff = target.getId();
        Vec3 from = entry == null ? start : entry;
        self.setPos(from.x + normal.x * APOLI$STAND_OFF, from.y + normal.y * APOLI$STAND_OFF,
            from.z + normal.z * APOLI$STAND_OFF);
        apoli$launch(self, reflected);
        this.apoli$missFired = false;
        config.hooks().bientityActionOnBounce().ifPresent(a -> a.run(BiEntityCtx.of(owner, self, level)));
        return true;
    }

    @Unique
    private static @Nullable Vec3 apoli$faceNormal(AABB box, Vec3 point) {
        if (Math.abs(point.x - box.minX) < APOLI$FACE_EPSILON) return APOLI$WEST;
        if (Math.abs(point.x - box.maxX) < APOLI$FACE_EPSILON) return APOLI$EAST;
        if (Math.abs(point.z - box.minZ) < APOLI$FACE_EPSILON) return APOLI$NORTH;
        if (Math.abs(point.z - box.maxZ) < APOLI$FACE_EPSILON) return APOLI$SOUTH;
        if (Math.abs(point.y - box.minY) < APOLI$FACE_EPSILON) return APOLI$DOWN;
        if (Math.abs(point.y - box.maxY) < APOLI$FACE_EPSILON) return APOLI$UP;
        return null;
    }

    @Unique
    private static void apoli$launch(Projectile self, Vec3 velocity) {
        self.setDeltaMovement(velocity);
        double horizontal = velocity.horizontalDistance();
        self.setYRot((float) (Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI)));
        self.setXRot((float) (Mth.atan2(velocity.y, horizontal) * (180.0 / Math.PI)));
        self.yRotO = self.getYRot();
        self.xRotO = self.getXRot();
        self.hasImpulse = true;
    }

    @Unique
    private void apoli$runOwnerAction(Projectile self, Optional<BiEntityAction> action) {
        if (action.isEmpty()) return;
        boolean attributed = dev.overgrown.apoli.attribution.PowerCause.push(
            this.apoli$causeHolder, this.apoli$causePower);
        try {
            action.get().run(BiEntityCtx.of(self.getOwner(), self, self.level()));
        } finally {
            if (attributed) dev.overgrown.apoli.attribution.PowerCause.pop();
        }
    }

    @Unique
    private static void apoli$standOff(Projectile self, BlockHitResult blockHit) {
        Direction face = blockHit.getDirection();
        Vec3 landing = blockHit.getLocation();
        double clearance = Math.max(self.getBbWidth(), self.getBbHeight()) * 0.5 + 0.01;
        self.setPos(landing.x + face.getStepX() * clearance,
            landing.y + face.getStepY() * clearance,
            landing.z + face.getStepZ() * clearance);
    }

    @Unique
    private boolean apoli$tryBounce(Projectile self, Level level, FireProjectilePower.Config config,
                                    HitResult result) {
        FireProjectilePower.Reflect reflect = config.reflective().orElse(null);
        if (reflect == null) return false;
        if (!(result instanceof BlockHitResult blockHit)) return false;
        Entity owner = self.getOwner();
        int limit = reflect.maxBounces().evalInt(owner);
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
            velocity.z - 2.0 * dot * face.getStepZ()).scale(reflect.speed().eval(owner));
        if (reflected.lengthSqr() < 1.0E-6) return false;

        this.apoli$bounces++;
        apoli$standOff(self, blockHit);
        apoli$launch(self, reflected);
        apoli$newLeg();
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
