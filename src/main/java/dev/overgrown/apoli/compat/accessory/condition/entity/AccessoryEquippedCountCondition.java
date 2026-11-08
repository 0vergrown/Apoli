package dev.overgrown.apoli.compat.accessory.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class AccessoryEquippedCountCondition implements ConditionType<EntityCtx, AccessoryEquippedCountCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo, List<AccessorySlot> slots, Optional<ItemCondition> itemCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo),
            AccessorySlot.LIST.optionalFieldOf("slots", List.of()).forGetter(Cfg::slots),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Cfg::itemCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.living();
        if (e == null) return false;
        int limit = cfg.compareTo() < 0 ? 1 : cfg.compareTo() == Integer.MAX_VALUE ? Integer.MAX_VALUE : cfg.compareTo() + 1;
        int count;
        if (cfg.itemCondition().isEmpty()) {
            count = Accessories.count(e, cfg.slots(), null, limit);
        } else {
            ItemCondition condition = cfg.itemCondition().get();
            count = Accessories.count(e, cfg.slots(), stack -> condition.test(new ItemCtx(stack, ctx.level(), e)), limit);
        }
        return cfg.comparison().compare(count, cfg.compareTo());
    }
}
