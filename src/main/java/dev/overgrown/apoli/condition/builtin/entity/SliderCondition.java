package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.power.builtin.GuiPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.OptionalDouble;

public final class SliderCondition implements ConditionType<EntityCtx, SliderCondition.Cfg> {

    public record Cfg(ResourceLocation gui, String slider, Comparison comparison, double compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("gui").forGetter(Cfg::gui),
            Codec.STRING.fieldOf("slider").forGetter(Cfg::slider),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.DOUBLE.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null) return false;
        OptionalDouble value = GuiPower.sliderValue(entity, cfg.gui(), cfg.slider());
        return value.isPresent() && cfg.comparison().compare(value.getAsDouble(), cfg.compareTo());
    }
}
