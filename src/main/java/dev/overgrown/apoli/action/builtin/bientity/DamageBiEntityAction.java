package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.data.ExpressionContext;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DamageBiEntityAction implements ActionType<BiEntityCtx, DamageBiEntityAction.Cfg> {
    public record Cfg(
        Optional<Float> amount,
        ResourceLocation damageType,
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers
    ) {
        public List<AttributeModifier> allModifiers() {
            if (modifier.isEmpty() && modifiers.isEmpty()) return List.of();
            if (modifier.isPresent() && modifiers.isEmpty()) return List.of(modifier.get());
            if (modifier.isEmpty()) return modifiers.get();
            List<AttributeModifier> combined = new java.util.ArrayList<>(modifiers.get().size() + 1);
            combined.add(modifier.get());
            combined.addAll(modifiers.get());
            return combined;
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("amount").forGetter(Cfg::amount),
            ResourceLocation.CODEC.fieldOf("damage_type").forGetter(Cfg::damageType),
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Cfg::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Cfg::modifiers)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity target = ctx.target();
        LivingEntity actor = ctx.actor();
        ResourceKey<DamageType> typeKey = ResourceKey.create(Registries.DAMAGE_TYPE, cfg.damageType);
        Optional<net.minecraft.core.Holder.Reference<DamageType>> holder = ctx.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE).getHolder(typeKey);
        if (holder.isEmpty()) return;
        DamageSource source = new DamageSource(holder.get(), actor);

        float base = cfg.amount.orElseGet(target::getMaxHealth);
        List<AttributeModifier> mods = cfg.allModifiers();
        float finalAmount = mods.isEmpty() ? base : applyModifiers(base, mods, target);
        if (finalAmount > 0f) target.hurt(source, finalAmount);
    }

    private static float applyModifiers(float base, List<AttributeModifier> mods, LivingEntity target) {
        Map<String, Double> vars = ExpressionContext.entityStats(target);
        Map<String, Double> resources = ExpressionContext.resourceValues(PowerContainer.of(target));
        double b = base;
        double total = base;
        mods.sort(Comparator
            .comparingInt((AttributeModifier m) -> m.operation().phase().ordinal())
            .thenComparingInt(m -> m.operation().order()));
        for (AttributeModifier mod : mods) {
            double modValue = mod.resolveInput(vars, resources);
            AttributeModifierOperation.Result r = mod.operation().apply(b, total, modValue);
            b = r.base();
            total = r.total();
            if (mod.operation().phase() == AttributeModifierOperation.Phase.BASE) total = b;
        }
        return Math.max(0f, (float) total);
    }
}
