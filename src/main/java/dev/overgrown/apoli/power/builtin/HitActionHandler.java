package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public final class HitActionHandler {
    private static final ResourceLocation ACTION_ON_HIT = Apoli.id("action_on_hit");
    private static final ResourceLocation ACTION_WHEN_HIT = Apoli.id("action_when_hit");

    private HitActionHandler() {}

    public static void fire(@Nullable LivingEntity attacker, LivingEntity target,
                            DamageSource source, float amount, Level level, boolean damageLanded) {
        if (!damageLanded || level.isClientSide()) return;
        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);

        if (attacker != null) {
            fireOnHit(attacker, target, source, amount, level, damageCtx);
        }
        fireWhenHit(attacker, target, source, amount, level, damageCtx);
    }

    private static void fireOnHit(LivingEntity attacker, LivingEntity target, DamageSource source,
                                  float amount, Level level, DamageCtx damageCtx) {
        PowerContainer container = PowerContainer.of(attacker);
        if (!(container instanceof PowerContainerImpl impl)) return;
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!ACTION_ON_HIT.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (!(power.config() instanceof ActionOnHitPower.Config cfg)) continue;
            if (!ready(impl, powerId)) continue;
            if (power.condition().isPresent()
                && !power.condition().get().test(new EntityCtx(attacker, level))) continue;

            BiEntityCtx biCtx = new BiEntityCtx(attacker, target, level);
            if (cfg.bientityCondition().isPresent()
                && !cfg.bientityCondition().get().test(biCtx)) continue;
            if (cfg.targetCondition().isPresent()
                && !cfg.targetCondition().get().test(new EntityCtx(target, level))) continue;
            if (cfg.damageCondition().isPresent()
                && !cfg.damageCondition().get().test(damageCtx)) continue;

            cfg.bientityAction().ifPresent(a -> a.run(biCtx));
            cfg.selfAction().ifPresent(a -> a.run(new EntityCtx(attacker, level)));
            cfg.targetAction().ifPresent(a -> a.run(new EntityCtx(target, level)));
            impl.setAuxInt(powerId, cfg.cooldown());
        }
    }

    private static void fireWhenHit(@Nullable LivingEntity attacker, LivingEntity target, DamageSource source,
                                    float amount, Level level, DamageCtx damageCtx) {
        PowerContainer container = PowerContainer.of(target);
        if (!(container instanceof PowerContainerImpl impl)) return;
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!ACTION_WHEN_HIT.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (!(power.config() instanceof ActionWhenHitPower.Config cfg)) continue;
            if (!ready(impl, powerId)) continue;
            if (power.condition().isPresent()
                && !power.condition().get().test(new EntityCtx(target, level))) continue;

            if (cfg.bientityCondition().isPresent()) {
                if (attacker == null) continue;
                if (!cfg.bientityCondition().get().test(new BiEntityCtx(attacker, target, level))) continue;
            }
            if (cfg.damageCondition().isPresent()
                && !cfg.damageCondition().get().test(damageCtx)) continue;

            if (attacker != null) {
                cfg.bientityAction().ifPresent(a -> a.run(new BiEntityCtx(attacker, target, level)));
                cfg.attackerAction().ifPresent(a -> a.run(new EntityCtx(attacker, level)));
            }
            cfg.selfAction().ifPresent(a -> a.run(new EntityCtx(target, level)));
            impl.setAuxInt(powerId, cfg.cooldown());
        }
    }

    private static boolean ready(PowerContainerImpl impl, ResourceLocation powerId) {
        OptionalInt cur = impl.getAuxInt(powerId);
        return cur.isEmpty() || cur.getAsInt() <= 0;
    }
}
