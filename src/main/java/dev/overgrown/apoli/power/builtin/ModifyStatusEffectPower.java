package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModifyStatusEffectPower extends PowerType<ModifyStatusEffectPower.Config> {
    public record Config(
        Optional<AttributeModifier> modifier,
        Optional<List<AttributeModifier>> modifiers,
        List<ResourceLocation> statusEffects
    ) {
        public boolean applies(ResourceLocation effectId) {
            if (statusEffects.isEmpty()) return true;
            for (int i = 0; i < statusEffects.size(); i++) {
                if (statusEffects.get(i).equals(effectId)) return true;
            }
            return false;
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            IdCodecs.ID.optionalFieldOf("status_effect").forGetter(c -> Optional.empty()),
            IdCodecs.ID.listOf().optionalFieldOf("status_effects", List.of()).forGetter(Config::statusEffects)
        ).apply(i, (modifier, modifiers, single, list) -> {
            if (single.isEmpty()) return new Config(modifier, modifiers, list);
            List<ResourceLocation> merged = new ArrayList<>(list);
            merged.add(single.get());
            return new Config(modifier, modifiers, List.copyOf(merged));
        }));
    }

    public static MobEffectInstance apply(LivingEntity entity, MobEffectInstance original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        boolean anyAmplifier = !container.powersOfType(ApoliIds.MODIFY_STATUS_EFFECT_AMPLIFIER).isEmpty();
        boolean anyDuration = !container.powersOfType(ApoliIds.MODIFY_STATUS_EFFECT_DURATION).isEmpty();
        if (!anyAmplifier && !anyDuration) return original;

        Holder<MobEffect> effect = original.getEffect();
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if (effectId == null) return original;

        int amplifier = anyAmplifier
            ? Math.round(collect(entity, ApoliIds.MODIFY_STATUS_EFFECT_AMPLIFIER, effectId, original.getAmplifier()))
            : original.getAmplifier();
        int duration = anyDuration
            ? Math.round(collect(entity, ApoliIds.MODIFY_STATUS_EFFECT_DURATION, effectId, original.getDuration()))
            : original.getDuration();

        if (amplifier == original.getAmplifier() && duration == original.getDuration()) return original;
        return new MobEffectInstance(effect, Math.max(0, duration), Math.max(0, amplifier),
            original.isAmbient(), original.isVisible(), original.showIcon());
    }

    private static float collect(LivingEntity entity, ResourceLocation typeId, ResourceLocation effectId, float base) {
        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, typeId, Config.class, cfg -> {
            if (!cfg.applies(effectId)) return;
            mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers()));
        });
        if (mods.isEmpty()) return base;
        return AttributeModifierHelper.apply(base, AttributeModifierHelper.ensureSorted(mods), entity);
    }
}
