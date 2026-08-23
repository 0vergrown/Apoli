package dev.overgrown.apoli.data;

import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.mixin.damage.LivingEntityLastHurtAccessor;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.ModifyDamageHandler;
import dev.overgrown.apoli.power.builtin.ModifyProjectileDamageHandler;
import dev.overgrown.apoli.power.builtin.PreventDeathPower;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class DamagePrediction {

    private DamagePrediction() {}

    public static boolean wouldKill(LivingEntity target, DamageSource source, float amount) {
        float health = target.getHealth();
        if (health <= 0.0F) return false;
        float dealt = predict(target, source, amount);
        if (dealt < health) return false;
        return !preventsDeath(target, source, amount);
    }

    public static float predict(LivingEntity target, DamageSource source, float amount) {
        Level level = target.level();
        if (target.isInvulnerableTo(source)) return 0.0F;
        if (target.isDeadOrDying()) return 0.0F;
        if (source.is(DamageTypeTags.IS_FIRE) && target.hasEffect(MobEffects.FIRE_RESISTANCE)) return 0.0F;

        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        float f = ModifyProjectileDamageHandler.previewAmount(attacker, target, source, amount, level);
        f = ModifyDamageHandler.previewAmount(attacker, target, source, f, level);
        if (f <= 0.0F) return 0.0F;

        if (target.isDamageSourceBlocked(source)) return 0.0F;

        if (source.is(DamageTypeTags.IS_FREEZING) && target.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            f *= 5.0F;
        }
        if (target.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            float lastHurt = ((LivingEntityLastHurtAccessor) target).apoli$getLastHurt();
            if (f <= lastHurt) return 0.0F;
            f -= lastHurt;
        }

        f = afterArmor(target, attacker, source, f, level);
        f = afterMagic(target, source, f, level);
        return Math.max(f - target.getAbsorptionAmount(), 0.0F);
    }

    private static float afterArmor(LivingEntity target, @Nullable LivingEntity attacker,
                                    DamageSource source, float f, Level level) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return f;
        if (!ModifyDamageHandler.shouldApplyArmor(target, attacker, source, f, level)) return f;
        return CombatRules.getDamageAfterAbsorb(f, target.getArmorValue(),
            (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    private static float afterMagic(LivingEntity target, DamageSource source, float f, Level level) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) return f;
        if (target.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int amplified = (target.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
            f = Math.max(f * (25 - amplified) / 25.0F, 0.0F);
        }
        if (f <= 0.0F) return 0.0F;
        if (source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) return f;
        int protection = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), source);
        return protection > 0 ? CombatRules.getDamageAfterMagicAbsorb(f, protection) : f;
    }

    private static boolean preventsDeath(LivingEntity target, DamageSource source, float amount) {
        PowerContainer container = PowerContainer.of(target);
        if (container == null || container.isEmpty()) return false;
        Level level = target.level();
        DamageCtx damageCtx = null;
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof PreventDeathPower)) continue;
            if (!(power.config() instanceof PreventDeathPower.Config cfg)) continue;
            if (power.condition().isPresent()
                && !power.condition().get().test(new EntityCtx(target, level))) continue;
            if (cfg.damageCondition().isPresent()) {
                if (damageCtx == null) damageCtx = new DamageCtx(source, target, level, amount);
                if (!cfg.damageCondition().get().test(damageCtx)) continue;
            }
            return true;
        }
        return false;
    }

    public static DamageSource source(Level level, ResourceLocation damageType, @Nullable net.minecraft.world.entity.Entity attacker) {
        var holder = level.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolder(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, damageType))
            .orElse(null);
        if (holder == null) return null;
        return attacker == null ? new DamageSource(holder) : new DamageSource(holder, attacker);
    }
}
