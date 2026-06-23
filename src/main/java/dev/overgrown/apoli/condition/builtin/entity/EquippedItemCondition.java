package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EquippedItemCondition implements ConditionType<EntityCtx, EquippedItemCondition.Cfg> {
    public record Cfg(EquipmentSlot equipmentSlot, ItemCondition itemCondition) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EquipmentSlot.CODEC.fieldOf("equipment_slot").forGetter(Cfg::equipmentSlot),
            ItemCondition.CODEC.fieldOf("item_condition").forGetter(Cfg::itemCondition)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.entity();
        ItemStack stack = entity.getItemBySlot(cfg.equipmentSlot.vanilla());
        return cfg.itemCondition.test(new ItemCtx(stack, ctx.level(), entity));
    }
}
