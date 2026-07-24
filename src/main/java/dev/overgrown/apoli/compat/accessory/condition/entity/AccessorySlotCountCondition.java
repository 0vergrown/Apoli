package dev.overgrown.apoli.compat.accessory.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class AccessorySlotCountCondition implements ConditionType<EntityCtx, AccessorySlotCountCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo, List<AccessorySlot> slots) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo),
            AccessorySlot.LIST.optionalFieldOf("slots", List.of()).forGetter(Cfg::slots)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.living();
        if (e == null) return false;
        int count = Accessories.slots(e, cfg.slots()).size();
        return cfg.comparison().compare(count, cfg.compareTo());
    }
}
