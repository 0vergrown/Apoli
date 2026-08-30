package dev.overgrown.apoli.entity;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ScareMobsPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class ScareMobsGoal extends Goal {

    private final PathfinderMob mob;
    private @Nullable LivingEntity scary;
    private double speed;
    private @Nullable Path fleePath;

    public ScareMobsGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        List<LivingEntity> holders = ScareMobsPower.holders(this.mob.level());
        if (holders.isEmpty()) return false;
        this.scary = findScary(holders);
        if (this.scary == null) return false;
        Vec3 away = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.scary.position());
        if (away == null) return false;
        if (this.scary.distanceToSqr(away.x, away.y, away.z) < this.scary.distanceToSqr(this.mob)) return false;
        this.fleePath = this.mob.getNavigation().createPath(away.x, away.y, away.z, 0);
        return this.fleePath != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.scary != null && this.scary.isAlive() && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.fleePath, this.speed);
    }

    @Override
    public void stop() {
        this.scary = null;
        this.fleePath = null;
    }

    private @Nullable LivingEntity findScary(List<LivingEntity> holders) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < holders.size(); i++) {
            LivingEntity candidate = holders.get(i);
            if (candidate == this.mob || candidate.level() != this.mob.level()) continue;
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
