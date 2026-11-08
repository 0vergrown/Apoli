package dev.overgrown.apoli.compat.accessory.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class EquippedAccessoryCondition implements ConditionType<EntityCtx, EquippedAccessoryCondition.Cfg> {
    public record Cfg(Optional<ItemCondition> itemCondition, List<AccessorySlot> slots) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(Cfg::itemCondition),
            LoggedOptionalField.strict("slots", AccessorySlot.LIST)
                .xmap(slots -> slots.orElse(List.of()), slots -> slots.isEmpty() ? Optional.empty() : Optional.of(slots))
                .forGetter(Cfg::slots)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null) return false;
        if (cfg.itemCondition().isEmpty()) return Accessories.anyEquipped(entity, cfg.slots(), null);
        ItemCondition condition = cfg.itemCondition().get();
        return Accessories.anyEquipped(entity, cfg.slots(), stack -> condition.test(new ItemCtx(stack, ctx.level(), entity)));
    }
}
