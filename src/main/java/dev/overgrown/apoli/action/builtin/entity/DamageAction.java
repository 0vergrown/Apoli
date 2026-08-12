package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class DamageAction implements ActionType<EntityCtx, DamageAction.Cfg> {
    public record Cfg(
        Optional<Expression> amount,
        ResourceLocation damageType,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.<Cfg>wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                dev.overgrown.apoli.codec.LoggedOptionalField.strict("amount", Expression.FLOAT_OR_EXPR).forGetter(Cfg::amount),
                IdCodecs.ID.fieldOf("damage_type").forGetter(Cfg::damageType),
                AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Cfg::modifier),
                AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Cfg::modifiers)
            ).apply(i, Cfg::new)),
            java.util.Map.of("damage", "amount"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity target = ctx.entity();
        LivingEntity livingTarget = ctx.living();
        if (cfg.amount.isEmpty() && livingTarget == null) return;
        Holder.Reference<DamageType> typeHolder = ctx.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, cfg.damageType))
            .orElse(null);
        if (typeHolder == null) return;
        DamageSource source = new DamageSource(typeHolder);
        float base = cfg.amount.isPresent() ? (float) cfg.amount.get().eval(target) : livingTarget.getMaxHealth();
        float amount = applyModifiers(base, cfg, livingTarget);
        target.hurt(source, amount);
    }

    private static float applyModifiers(float base, Cfg cfg, LivingEntity target) {
        List<AttributeModifier> all = AttributeModifierHelper.flatten(cfg.modifier, cfg.modifiers);
        if (all.isEmpty()) return base;
        return AttributeModifierHelper.apply(base, all, target);
    }
}
