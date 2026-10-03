package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class ModifyFovPower extends PowerType<ModifyFovPower.Config> {
    public static final ResourceLocation CANONICAL = Apoli.id("modify_fov");

    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers,
                         boolean affectedByFovEffectScale,
                         List<AttributeModifier> flattened) {
        Config(Optional<AttributeModifier> modifier, Optional<List<AttributeModifier>> modifiers,
               boolean affectedByFovEffectScale) {
            this(modifier, modifiers, affectedByFovEffectScale, AttributeModifierHelper.flatten(modifier, modifiers));
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers),
            Codec.BOOL.optionalFieldOf("affected_by_fov_effect_scale", true).forGetter(Config::affectedByFovEffectScale)
        ).apply(i, Config::new));
    }

    @Override
    public boolean resolvesForClient() {
        return true;
    }

    public static float apply(@Nullable Entity holder, float value, Predicate<ResourceLocation> active) {
        if (holder == null) return value;
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return value;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        if (powers.isEmpty()) return value;
        double result = value;
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (!active.test(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg) || cfg.flattened().isEmpty()) continue;
            result = AttributeModifierHelper.apply(result, cfg.flattened(), holder, container);
        }
        return (float) result;
    }

    public static boolean ignoresEffectScale(@Nullable Entity holder, Predicate<ResourceLocation> active) {
        if (holder == null) return false;
        PowerContainer container = PowerContainer.of(holder);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        for (int i = 0, n = powers.size(); i < n; i++) {
            ResourceLocation powerId = powers.get(i);
            if (!active.test(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power != null && power.config() instanceof Config cfg && !cfg.affectedByFovEffectScale()) return true;
        }
        return false;
    }
}
