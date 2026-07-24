package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.Entity;

public final class DistanceCondition implements ConditionType<BiEntityCtx, DistanceCondition.Cfg> {
    public record Cfg(Comparison comparison, double value) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.DOUBLE.fieldOf("compare_to").forGetter(Cfg::value)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.rawActor();
        Entity target = ctx.rawTarget();
        if (actor == null || target == null) return false;
        return cfg.comparison.compare(actor.distanceTo(target), cfg.value);
    }
}
