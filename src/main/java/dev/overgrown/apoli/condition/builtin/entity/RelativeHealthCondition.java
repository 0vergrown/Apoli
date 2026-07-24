package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.LivingEntity;

public final class RelativeHealthCondition implements ConditionType<EntityCtx, RelativeHealthCondition.Cfg> {
    public record Cfg(Comparison comparison, float compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.living();
        if (e == null || e.getMaxHealth() <= 0) return false;
        float ratio = e.getHealth() / e.getMaxHealth();
        return cfg.comparison.compare(ratio, cfg.compareTo);
    }
}
