package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ModifyProjectileDamageHandler {
    private static final ResourceLocation CANONICAL = Apoli.id("modify_projectile_damage");

    private ModifyProjectileDamageHandler() {}

    public static float modifyAmount(@Nullable LivingEntity shooter, LivingEntity target,
                                     DamageSource source, float amount, Level level) {
        return apply(shooter, target, source, amount, level, true);
    }

    public static float previewAmount(@Nullable LivingEntity shooter, LivingEntity target,
                                      DamageSource source, float amount, Level level) {
        return apply(shooter, target, source, amount, level, false);
    }

    private static float apply(@Nullable LivingEntity shooter, LivingEntity target,
                               DamageSource source, float amount, Level level, boolean runActions) {
        if (shooter == null && source.getDirectEntity() instanceof Projectile p
            && p.getOwner() instanceof LivingEntity owner) {
            shooter = owner;
        }
        if (shooter == null) return amount;
        if (!isProjectile(source) && !fromActiveProjectile(shooter)) return amount;
        PowerContainer container = PowerContainer.of(shooter);
        if (container == null) return amount;

        DamageCtx damageCtx = new DamageCtx(source, target, level, amount);
        List<ModifyProjectileDamagePower.Config> matches = new ArrayList<>();
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!CANONICAL.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (!(power.config() instanceof ModifyProjectileDamagePower.Config cfg)) continue;
            if (power.condition().isPresent()
                && !power.condition().get().test(new EntityCtx(shooter, level))) continue;
            if (cfg.damageCondition().isPresent()
                && !cfg.damageCondition().get().test(damageCtx)) continue;
            if (cfg.targetCondition().isPresent()
                && !cfg.targetCondition().get().test(new EntityCtx(target, level))) continue;
            matches.add(cfg);
        }
        if (matches.isEmpty()) return amount;

        List<AttributeModifier> allMods = new ArrayList<>();
        for (ModifyProjectileDamagePower.Config cfg : matches) {
            allMods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        }
        float modified = AttributeModifierHelper.apply(amount, allMods, shooter);

        if (runActions) {
            final LivingEntity finalShooter = shooter;
            for (ModifyProjectileDamagePower.Config cfg : matches) {
                cfg.selfAction().ifPresent(a -> a.run(new EntityCtx(finalShooter, level)));
                cfg.targetAction().ifPresent(a -> a.run(new EntityCtx(target, level)));
            }
        }
        return Math.max(0f, modified);
    }

    private static boolean isProjectile(DamageSource source) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return true;
        return source.getDirectEntity() instanceof Projectile;
    }

    private static Projectile activeProjectile;

    public static void beginProjectileContext(Projectile projectile) {
        activeProjectile = projectile;
    }

    public static void endProjectileContext() {
        activeProjectile = null;
    }

    private static boolean fromActiveProjectile(LivingEntity shooter) {
        return activeProjectile != null && activeProjectile.getOwner() == shooter;
    }
}
