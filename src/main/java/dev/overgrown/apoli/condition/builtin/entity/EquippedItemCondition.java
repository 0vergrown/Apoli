package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class EquippedItemCondition implements ConditionType<EntityCtx, EquippedItemCondition.Cfg> {
    public record Cfg(Optional<EquipmentSlot> equipmentSlot, List<AccessorySlot> accessorySlot, ItemCondition itemCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
            LoggedOptionalField.strict("equipment_slot", EquipmentSlot.CODEC).forGetter(Cfg::equipmentSlot),
            LoggedOptionalField.strict("accessory_slot", AccessorySlot.LIST)
                .xmap(slots -> slots.orElse(List.of()), slots -> slots.isEmpty() ? Optional.empty() : Optional.of(slots))
                .forGetter(Cfg::accessorySlot),
            ItemCondition.CODEC.fieldOf("item_condition").forGetter(Cfg::itemCondition)
        ).apply(i, Cfg::new)).flatXmap(EquippedItemCondition::validate, DataResult::success);
    }

    private static DataResult<Cfg> validate(Cfg cfg) {
        if (cfg.equipmentSlot().isPresent() || !cfg.accessorySlot().isEmpty()) return DataResult.success(cfg);
        return DataResult.error(() -> "equipped_item needs an 'equipment_slot', an 'accessory_slot', or both");
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null) return false;
        if (cfg.equipmentSlot.isPresent()) {
            ItemStack stack = entity.getItemBySlot(cfg.equipmentSlot.get().vanilla());
            if (cfg.itemCondition.test(new ItemCtx(stack, ctx.level(), entity))) return true;
        }
        if (cfg.accessorySlot.isEmpty() || !Accessories.anyPresent()) return false;
        ItemCondition condition = cfg.itemCondition;
        return Accessories.anyEquipped(entity, cfg.accessorySlot, stack -> condition.test(new ItemCtx(stack, ctx.level(), entity)));
    }
}
