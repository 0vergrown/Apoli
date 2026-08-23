package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.data.DamagePrediction;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class DamageWouldKillBiEntityCondition implements ConditionType<BiEntityCtx, DamageWouldKillBiEntityCondition.Cfg> {
    public record Cfg(
        Expression amount,
        ResourceLocation damageType,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.<Cfg>wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.FLOAT_OR_EXPR.fieldOf("amount").forGetter(Cfg::amount),
                IdCodecs.ID.fieldOf("damage_type").forGetter(Cfg::damageType),
                AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Cfg::modifier),
                AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Cfg::modifiers)
            ).apply(i, Cfg::new)),
            java.util.Map.of("damage", "amount"));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity target = ctx.livingTarget();
        if (target == null) return false;
        DamageSource source = DamagePrediction.source(ctx.level(), cfg.damageType, ctx.actor());
        if (source == null) return false;
        float base = (float) cfg.amount.eval(ctx.actor());
        List<AttributeModifier> mods = AttributeModifierHelper.flatten(cfg.modifier, cfg.modifiers);
        float amount = mods.isEmpty() ? base : Math.max(0f, AttributeModifierHelper.apply(base, mods, target));
        return DamagePrediction.wouldKill(target, source, amount);
    }
}
