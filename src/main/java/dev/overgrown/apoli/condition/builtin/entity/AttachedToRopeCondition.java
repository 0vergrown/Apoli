package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.rope.Rope;
import dev.overgrown.apoli.rope.RopeManager;
import net.minecraft.world.entity.Entity;

import java.util.Optional;


public final class AttachedToRopeCondition implements ConditionType<EntityCtx, AttachedToRopeCondition.Cfg> {

    public record Cfg(Optional<String> slot, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("slot").forGetter(Cfg::slot),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 1).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        if (entity == null) return false;
        int count = 0;
        for (Rope rope : RopeManager.forEntity(entity.getUUID())) {
            if (cfg.slot.isPresent() && !cfg.slot.get().equals(rope.slot)) continue;
            count++;
        }
        return cfg.comparison.compare(count, cfg.compareTo);
    }
}
