package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.builtin.FireProjectilePower;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ProjectileHoming {
    public static final int NONE = -1;
    public static final int FINISHED = -2;

    private static final int SEARCH_INTERVAL = 4;
    private static final double KEEP_RANGE_FACTOR = 1.5;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);

    private final Optional<BiEntityCondition> condition;
    private final int delay;
    private final int duration;
    private final double range;
    private final double minCos;
    private final float turnRate;
    private int age;
    private int nextSearch;

    public ProjectileHoming(FireProjectilePower.Homing config, Entity shooter) {
        this.condition = config.bientityCondition();
        this.delay = Math.max(0, config.delay().evalInt(shooter));
        this.duration = Math.max(0, config.duration().evalInt(shooter));
        this.range = Math.max(0.0, config.range().eval(shooter));
        this.minCos = Math.cos(Math.toRadians(Mth.clamp(config.angle().eval(shooter), 0.0, 180.0)));
        this.turnRate = (float) Math.toRadians(Math.max(0.0, config.turnRate().eval(shooter)));
    }

    public float turnRate() {
        return this.turnRate;
    }

    public int update(CustomProjectileEntity projectile, int currentId) {
        int tick = this.age++;
        if (tick < this.delay) return NONE;
        if (this.duration > 0 && tick >= this.delay + this.duration) return FINISHED;
        Entity owner = projectile.getOwner();
        if (currentId >= 0) {
            Entity current = projectile.level().getEntity(currentId);
            if (current != null && this.keeps(projectile, owner, current)) return currentId;
        }
        if (tick < this.nextSearch) return NONE;
        this.nextSearch = tick + SEARCH_INTERVAL;
        Entity found = this.find(projectile, owner);
        return found == null ? NONE : found.getId();
    }

    private boolean keeps(CustomProjectileEntity projectile, @Nullable Entity owner, Entity target) {
        double keep = this.range * KEEP_RANGE_FACTOR;
        return target.level() == projectile.level()
            && target.distanceToSqr(projectile) <= keep * keep
            && this.accepts(projectile, owner, target);
    }

    @Nullable
    private Entity find(CustomProjectileEntity projectile, @Nullable Entity owner) {
        if (this.range <= 0.0) return null;
        Vec3 origin = projectile.getBoundingBox().getCenter();
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = velocity.length();
        boolean cone = this.minCos > -1.0 && speed > 1.0E-6;
        List<Entity> candidates = projectile.level().getEntities(projectile,
            new AABB(origin, origin).inflate(this.range), entity -> entity instanceof LivingEntity);
        Entity best = null;
        double bestDistance = this.range * this.range;
        for (int i = 0, n = candidates.size(); i < n; i++) {
            Entity candidate = candidates.get(i);
            Vec3 offset = candidate.getBoundingBox().getCenter().subtract(origin);
            double distance = offset.lengthSqr();
            if (distance > bestDistance) continue;
            if (cone && distance > 1.0E-6 && offset.dot(velocity) < this.minCos * Math.sqrt(distance) * speed) continue;
            if (!this.accepts(projectile, owner, candidate)) continue;
            best = candidate;
            bestDistance = distance;
        }
        return best;
    }

    private boolean accepts(CustomProjectileEntity projectile, @Nullable Entity owner, Entity candidate) {
        if (!(candidate instanceof LivingEntity living) || !living.isAlive() || candidate.isSpectator()) return false;
        if (owner != null) {
            if (candidate == owner || owner.isAlliedTo(candidate)) return false;
            if (candidate instanceof OwnableEntity ownable) {
                UUID ownerId = ownable.getOwnerUUID();
                if (ownerId != null && ownerId.equals(owner.getUUID())) return false;
            }
        }
        if (!projectile.canTarget(candidate)) return false;
        return this.condition.isEmpty()
            || this.condition.get().test(BiEntityCtx.of(owner != null ? owner : projectile, candidate, projectile.level()));
    }

    public static void steer(Entity projectile, Entity target, float turnRate) {
        if (turnRate <= 0.0F) return;
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 1.0E-6) return;
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(projectile.getBoundingBox().getCenter());
        double distance = toTarget.length();
        if (distance < 1.0E-6) return;
        Vec3 current = velocity.scale(1.0 / speed);
        Vec3 desired = toTarget.scale(1.0 / distance);
        double angle = Math.acos(Mth.clamp(current.dot(desired), -1.0, 1.0));
        Vec3 direction;
        if (angle <= turnRate) {
            direction = desired;
        } else {
            Vec3 axis = current.cross(desired);
            if (axis.lengthSqr() < 1.0E-12) {
                axis = current.cross(Math.abs(current.y) < 0.99 ? UP : EAST);
            }
            axis = axis.normalize();
            direction = current.scale(Math.cos(turnRate)).add(axis.cross(current).scale(Math.sin(turnRate)));
        }
        projectile.setDeltaMovement(direction.scale(speed));
    }
}
