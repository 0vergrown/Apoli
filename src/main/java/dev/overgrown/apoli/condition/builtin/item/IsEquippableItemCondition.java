package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.world.item.Equipable;

import java.util.Optional;

public final class IsEquippableItemCondition implements ConditionType<ItemCtx, IsEquippableItemCondition.Cfg> {
    public record Cfg(Optional<EquipmentSlot> equipmentSlot) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EquipmentSlot.CODEC.optionalFieldOf("equipment_slot").forGetter(Cfg::equipmentSlot)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        Equipable equipable = Equipable.get(ctx.stack());
        if (equipable == null) return false;
        if (cfg.equipmentSlot.isEmpty()) return true;
        return equipable.getEquipmentSlot() == cfg.equipmentSlot.get().vanilla();
    }
}
