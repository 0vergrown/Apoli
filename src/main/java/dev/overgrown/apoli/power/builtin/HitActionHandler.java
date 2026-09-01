package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.HitSide;
import dev.overgrown.apoli.data.expr.ExprDamageContext;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class HitActionHandler {

    private HitActionHandler() {}

    public static void fire(@Nullable LivingEntity attacker, LivingEntity target,
                            DamageSource source, float amount, Level level, boolean damageLanded) {
        if (!damageLanded || level.isClientSide()) return;
        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);

        long now = level.getGameTime();
        if (attacker != null) {
            fireOnHit(attacker, target, amount, level, damageCtx, now);
        }
        fireWhenHit(attacker, target, amount, level, damageCtx, now);
    }

    private static void fireOnHit(LivingEntity attacker, LivingEntity target,
                                  float amount, Level level, DamageCtx damageCtx, long now) {
        PowerContainer container = PowerContainer.of(attacker);
        if (!(container instanceof PowerContainerImpl impl)) return;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ACTION_ON_HIT);
        if (powers.isEmpty()) return;

        EntityCtx attackerCtx = null;
        EntityCtx targetCtx = null;
        BiEntityCtx biCtx = null;

        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof ActionOnHitPower.Config cfg)) continue;
            if (!ready(impl, powerId, now)) continue;

            if (attackerCtx == null) attackerCtx = new EntityCtx(attacker, level);
            if (power.condition().isPresent() && !power.condition().get().test(attackerCtx)) continue;

            if (biCtx == null) biCtx = new BiEntityCtx(attacker, target, level);
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(biCtx)) continue;
            if (cfg.targetCondition().isPresent()) {
                if (targetCtx == null) targetCtx = new EntityCtx(target, level);
                if (!cfg.targetCondition().get().test(targetCtx)) continue;
            }
            if (cfg.attackerCondition().isPresent() && !cfg.attackerCondition().get().test(attackerCtx)) continue;
            if (cfg.damageCondition().isPresent() && !cfg.damageCondition().get().test(damageCtx)) continue;

            if (targetCtx == null) targetCtx = new EntityCtx(target, level);

            double prevDamage = ExprDamageContext.set(amount);
            try {
                run(cfg.bientityAction(), biCtx);
                run(cfg.selfAction(), attackerCtx);
                run(cfg.attackerAction(), attackerCtx);
                run(cfg.targetAction(), targetCtx);
                run(cfg.entityAction(), cfg.entityActionTarget() == HitSide.TARGET ? targetCtx : attackerCtx);
            } finally {
                ExprDamageContext.restore(prevDamage);
            }
            impl.setAuxInt(powerId, expiry(now, dev.overgrown.apoli.power.PowerResources.cooldownTicks(cfg.cooldown(), impl)));
        }
    }

    private static void fireWhenHit(@Nullable LivingEntity attacker, LivingEntity target,
                                    float amount, Level level, DamageCtx damageCtx, long now) {
        PowerContainer container = PowerContainer.of(target);
        if (!(container instanceof PowerContainerImpl impl)) return;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ACTION_WHEN_HIT);
        if (powers.isEmpty()) return;

        EntityCtx targetCtx = null;
        EntityCtx attackerCtx = null;
        BiEntityCtx biCtx = null;

        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof ActionWhenHitPower.Config cfg)) continue;
            if (!ready(impl, powerId, now)) continue;

            if (targetCtx == null) targetCtx = new EntityCtx(target, level);
            if (power.condition().isPresent() && !power.condition().get().test(targetCtx)) continue;

            if (cfg.bientityCondition().isPresent()) {
                if (attacker == null) continue;
                if (biCtx == null) biCtx = new BiEntityCtx(attacker, target, level);
                if (!cfg.bientityCondition().get().test(biCtx)) continue;
            }
            if (cfg.attackerCondition().isPresent()) {
                if (attacker == null) continue;
                if (attackerCtx == null) attackerCtx = new EntityCtx(attacker, level);
                if (!cfg.attackerCondition().get().test(attackerCtx)) continue;
            }
            if (cfg.targetCondition().isPresent() && !cfg.targetCondition().get().test(targetCtx)) continue;
            if (cfg.damageCondition().isPresent() && !cfg.damageCondition().get().test(damageCtx)) continue;

            double prevDamage = ExprDamageContext.set(amount);
            try {
                if (attacker != null) {
                    if (biCtx == null) biCtx = new BiEntityCtx(attacker, target, level);
                    if (attackerCtx == null) attackerCtx = new EntityCtx(attacker, level);
                    run(cfg.bientityAction(), biCtx);
                    run(cfg.attackerAction(), attackerCtx);
                }
                run(cfg.selfAction(), targetCtx);
                run(cfg.targetAction(), targetCtx);
                if (cfg.entityActionTarget() == HitSide.ATTACKER) {
                    if (attackerCtx != null) run(cfg.entityAction(), attackerCtx);
                } else {
                    run(cfg.entityAction(), targetCtx);
                }
            } finally {
                ExprDamageContext.restore(prevDamage);
            }
            impl.setAuxInt(powerId, expiry(now, dev.overgrown.apoli.power.PowerResources.cooldownTicks(cfg.cooldown(), impl)));
        }
    }

    private static void run(Optional<EntityAction> action, EntityCtx ctx) {
        if (action.isPresent()) action.get().run(ctx);
    }

    private static void run(Optional<dev.overgrown.apoli.action.BiEntityAction> action, BiEntityCtx ctx) {
        if (action.isPresent()) action.get().run(ctx);
    }

    static boolean ready(PowerContainerImpl impl, ResourceLocation powerId, long now) {
        OptionalInt cur = impl.getAuxInt(powerId);
        return cur.isEmpty() || cur.getAsInt() <= (int) now;
    }

    static int expiry(long now, int cooldown) {
        return (int) now + Math.max(cooldown, 0);
    }
}
