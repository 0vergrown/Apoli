package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.data.ExpressionContext;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ModifyDamageHandler {
    private ModifyDamageHandler() {}

    public static float modifyAmount(@Nullable LivingEntity attacker, LivingEntity target,
                                     DamageSource source, float amount, Level level) {
        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);
        List<Match> matches = new ArrayList<>();
        collect(matches, target, attacker, target, damageCtx, true);
        if (attacker != null) {
            collect(matches, attacker, attacker, target, damageCtx, false);
        }
        if (matches.isEmpty()) return amount;

        float modified = applyModifiers(amount, matches);
        for (Match m : matches) runActions(m, attacker, target, level);
        return modified;
    }

    public static float previewAmount(@Nullable LivingEntity attacker, LivingEntity target,
                                      DamageSource source, float amount, Level level) {
        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);
        List<Match> matches = new ArrayList<>();
        collect(matches, target, attacker, target, damageCtx, true);
        if (attacker != null) {
            collect(matches, attacker, attacker, target, damageCtx, false);
        }
        if (matches.isEmpty()) return amount;
        return applyModifiers(amount, matches);
    }

    public static boolean shouldApplyArmor(LivingEntity target, @Nullable LivingEntity attacker,
                                           DamageSource source, float amount, Level level) {
        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);
        List<Match> matches = new ArrayList<>();
        collect(matches, target, attacker, target, damageCtx, true);
        for (Match m : matches) {
            ModifyDamagePower.Config cfg = m.cfg;
            if (cfg.applyArmorCondition().isEmpty()) continue;
            if (!cfg.applyArmorCondition().get().test(new EntityCtx(target, level))) return false;
        }
        return true;
    }

    public static boolean shouldDamageArmor(LivingEntity target, @Nullable LivingEntity attacker,
                                            DamageSource source, float amount, Level level) {
        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);
        List<Match> matches = new ArrayList<>();
        collect(matches, target, attacker, target, damageCtx, true);
        for (Match m : matches) {
            ModifyDamagePower.Config cfg = m.cfg;
            if (cfg.damageArmorCondition().isEmpty()) continue;
            if (!cfg.damageArmorCondition().get().test(new EntityCtx(target, level))) return false;
        }
        return true;
    }

    private static void collect(List<Match> out, LivingEntity holder, @Nullable LivingEntity attacker,
                                LivingEntity target, DamageCtx damageCtx, boolean targetUsedSide) {
        PowerContainer container = PowerContainer.of(holder);
        if (container == null) return;
        ResourceLocation canonical = Apoli.id("modify_damage");
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!canonical.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (!(power.config() instanceof ModifyDamagePower.Config cfg)) continue;
            if (cfg.targetUsed() != targetUsedSide) continue;

            Level level = damageCtx.level();
            if (power.condition().isPresent()
                && !power.condition().get().test(new EntityCtx(holder, level))) continue;

            LivingEntity bientityActor = targetUsedSide ? attacker : holder;
            LivingEntity bientityTarget = targetUsedSide ? holder : target;
            if (cfg.bientityCondition().isPresent()) {
                if (bientityActor == null) continue;
                if (!cfg.bientityCondition().get().test(new BiEntityCtx(bientityActor, bientityTarget, level))) continue;
            }
            if (cfg.targetCondition().isPresent()
                && !cfg.targetCondition().get().test(new EntityCtx(bientityTarget, level))) continue;
            if (cfg.damageCondition().isPresent()
                && !cfg.damageCondition().get().test(damageCtx)) continue;

            out.add(new Match(cfg, bientityActor, bientityTarget, targetUsedSide));
        }
    }

    private static float applyModifiers(float amount, List<Match> matches) {
        List<AttributeModifier> allMods = new ArrayList<>();
        for (Match m : matches) allMods.addAll(m.cfg.allModifiers());
        if (allMods.isEmpty()) return amount;

        Map<String, Double> vars = matches.get(0).bientityTarget != null
            ? ExpressionContext.entityStats(matches.get(0).bientityTarget)
            : Map.of();
        Map<String, Double> resources = matches.get(0).bientityTarget != null
            ? ExpressionContext.resourceValues(PowerContainer.of(matches.get(0).bientityTarget))
            : Map.of();

        double base = amount;
        double total = amount;
        allMods.sort(Comparator
            .comparingInt((AttributeModifier m) -> m.operation().phase().ordinal())
            .thenComparingInt(m -> m.operation().order()));
        for (AttributeModifier mod : allMods) {
            double modValue = mod.resolveInput(vars, resources);
            AttributeModifierOperation.Result r = mod.operation().apply(base, total, modValue);
            base = r.base();
            total = r.total();
            if (mod.operation().phase() == AttributeModifierOperation.Phase.BASE) {
                total = base;
            }
        }
        return Math.max(0f, (float) total);
    }

    private static void runActions(Match m, @Nullable LivingEntity attacker, LivingEntity target, Level level) {
        ModifyDamagePower.Config cfg = m.cfg;
        if (cfg.bientityAction().isPresent() && m.bientityActor != null) {
            cfg.bientityAction().get().run(new BiEntityCtx(m.bientityActor, m.bientityTarget, level));
        }
        if (cfg.selfAction().isPresent()) {
            LivingEntity holder = m.targetUsedSide ? target : (attacker != null ? attacker : target);
            cfg.selfAction().get().run(new EntityCtx(holder, level));
        }
        if (!m.targetUsedSide && cfg.targetAction().isPresent()) {
            cfg.targetAction().get().run(new EntityCtx(target, level));
        }
        if (m.targetUsedSide && cfg.attackerAction().isPresent() && attacker != null) {
            cfg.attackerAction().get().run(new EntityCtx(attacker, level));
        }
    }

    private record Match(ModifyDamagePower.Config cfg, @Nullable LivingEntity bientityActor,
                         LivingEntity bientityTarget, boolean targetUsedSide) {}
}
