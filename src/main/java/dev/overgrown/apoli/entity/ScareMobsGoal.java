package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ScareMobsPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class ScareMobsGoal extends Goal {

    private static final double SEARCH_RANGE = 16.0;

    private final PathfinderMob mob;
    private @Nullable LivingEntity scary;
    private double speed;
    private @Nullable Vec3 fleeTarget;

    public ScareMobsGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!ScareMobsPower.anyHolders()) return false;
        this.scary = findScary();
        if (this.scary == null) return false;
        this.fleeTarget = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.scary.position());
        return this.fleeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.scary != null && this.scary.isAlive() && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.fleeTarget != null) {
            this.mob.getNavigation().moveTo(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, this.speed);
        }
    }

    @Override
    public void stop() {
        this.scary = null;
        this.fleeTarget = null;
    }

    private @Nullable LivingEntity findScary() {
        AABB box = this.mob.getBoundingBox().inflate(SEARCH_RANGE, SEARCH_RANGE / 2.0, SEARCH_RANGE);
        List<LivingEntity> candidates = this.mob.level().getEntitiesOfClass(LivingEntity.class, box,
            candidate -> candidate != this.mob && candidate.isAlive() && hasScarePower(candidate));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            LivingEntity candidate = candidates.get(i);
            double distance = candidate.distanceToSqr(this.mob);
            if (distance >= bestDistance) continue;
            double appliedSpeed = scareSpeed(candidate, distance);
            if (appliedSpeed <= 0.0) continue;
            best = candidate;
            bestDistance = distance;
            this.speed = appliedSpeed;
        }
        return best;
    }

    private static boolean hasScarePower(LivingEntity candidate) {
        PowerContainer container = PowerContainer.of(candidate);
        return container != null && !container.isEmpty()
            && !container.powersOfType(ApoliIds.SCARE_MOBS).isEmpty();
    }

    private double scareSpeed(LivingEntity candidate, double distanceSqr) {
        double[] speed = {0.0};
        PowerLookup.forEach(candidate, ApoliIds.SCARE_MOBS, ScareMobsPower.Config.class, cfg -> {
            if (speed[0] > 0.0) return;
            if (distanceSqr > cfg.radius() * cfg.radius()) return;
            if (cfg.bientityCondition().isPresent()
                && !cfg.bientityCondition().get().test(
                    new BiEntityCtx(candidate, this.mob, candidate.level()))) return;
            speed[0] = cfg.speed();
        });
        return speed[0];
    }
}
