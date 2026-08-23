package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ModifyAirSpeedPower extends PowerType<ModifyAirSpeedPower.Config> {
    public record Config(Optional<AttributeModifier> modifier,
                         Optional<List<AttributeModifier>> modifiers) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Config::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Config::modifiers)
        ).apply(i, Config::new));
    }

    public static float modify(LivingEntity entity, float original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        if (container.powersOfType(ApoliIds.MODIFY_AIR_SPEED).isEmpty()) return original;

        List<AttributeModifier> mods = new ArrayList<>();
        PowerLookup.forEach(entity, ApoliIds.MODIFY_AIR_SPEED, Config.class, cfg ->
            mods.addAll(AttributeModifierHelper.flatten(cfg.modifier(), cfg.modifiers())));
        if (mods.isEmpty()) return original;
        return AttributeModifierHelper.apply(original, AttributeModifierHelper.ensureSorted(mods), entity);
    }
}
