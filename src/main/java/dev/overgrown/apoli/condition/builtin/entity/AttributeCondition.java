package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public final class AttributeCondition implements ConditionType<EntityCtx, AttributeCondition.Cfg> {
    public record Cfg(ResourceLocation attribute, Comparison comparison, float compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("attribute").forGetter(Cfg::attribute),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Attribute attr = BuiltInRegistries.ATTRIBUTE.get(cfg.attribute);
        if (attr == null) return false;
        AttributeInstance inst = ctx.entity().getAttribute(attr);
        if (inst == null) return false;
        return cfg.comparison.compare((float) inst.getValue(), cfg.compareTo);
    }
}
