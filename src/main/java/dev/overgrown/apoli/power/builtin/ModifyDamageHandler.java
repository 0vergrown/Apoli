package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.dev.DevMode;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ModifyDamageHandler {
    private static final int DEV_REPORT_TICKS = 10;

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
        if (DevMode.any()) reportToDevMode(attacker, target, source, amount, modified, matches);
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
        ResourceLocation canonical = dev.overgrown.apoli.power.ApoliIds.MODIFY_DAMAGE;
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

            out.add(new Match(powerId, cfg, holder, container, bientityActor, bientityTarget, targetUsedSide));
        }
    }

    private static float applyModifiers(float amount, List<Match> matches) {
        List<AttributeModifierHelper.Owned> owned = null;
        for (int i = 0, n = matches.size(); i < n; i++) {
            Match m = matches.get(i);
            List<AttributeModifier> mods = m.cfg.allModifiers();
            for (int j = 0, k = mods.size(); j < k; j++) {
                if (owned == null) owned = new ArrayList<>(4);
                owned.add(new AttributeModifierHelper.Owned(mods.get(j), m.holder, m.container,
                    m.bientityActor, m.bientityTarget));
            }
        }
        if (owned == null) return amount;
        return Math.max(0f, (float) AttributeModifierHelper.applyOwned(amount, owned));
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

    private static void reportToDevMode(@Nullable LivingEntity attacker, LivingEntity target, DamageSource source,
                                        float before, float after, List<Match> matches) {
        StringBuilder line = new StringBuilder("damage ").append(number(before)).append(" → ").append(number(after));
        source.typeHolder().unwrapKey().ifPresent(type -> line.append(" (").append(type.location()).append(')'));
        appendSide(line, " · dealt: ", matches, false);
        appendSide(line, " · taken: ", matches, true);
        DevMode.reportEvery(target, new DevTopic(attacker == null ? null : attacker.getUUID()), DEV_REPORT_TICKS, line.toString());
    }

    private static void appendSide(StringBuilder line, String label, List<Match> matches, boolean takenSide) {
        boolean first = true;
        for (int i = 0, n = matches.size(); i < n; i++) {
            Match m = matches.get(i);
            if (m.targetUsedSide != takenSide) continue;
            line.append(first ? label : ", ").append(m.powerId);
            if (!takenSide) line.append(" (").append(m.holder.getName().getString()).append(')');
            first = false;
        }
    }

    private static String number(float value) {
        if (!Float.isFinite(value)) return Float.toString(value);
        if (value == Math.rint(value) && Math.abs(value) < 1.0E15F) return Long.toString((long) value);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private record DevTopic(@Nullable UUID attacker) {}

    private record Match(ResourceLocation powerId, ModifyDamagePower.Config cfg, LivingEntity holder,
                         PowerContainer container, @Nullable LivingEntity bientityActor,
                         LivingEntity bientityTarget, boolean targetUsedSide) {}
}
