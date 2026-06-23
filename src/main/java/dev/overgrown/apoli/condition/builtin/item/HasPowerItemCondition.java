package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class HasPowerItemCondition implements ConditionType<ItemCtx, HasPowerItemCondition.Cfg> {
    public record Cfg(ResourceLocation power, Optional<EquipmentSlot> slot) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("power").forGetter(Cfg::power),
            EquipmentSlot.CODEC.optionalFieldOf("slot").forGetter(Cfg::slot)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        ListTag powers = readPowerList(ctx);
        if (powers == null) return false;
        String wantedId = cfg.power.toString();
        String wantedSlot = cfg.slot.map(EquipmentSlot::getSerializedName).orElse(null);
        for (Tag entry : powers) {
            if (!(entry instanceof CompoundTag power)) continue;
            if (!wantedId.equals(power.getString("Id"))) continue;
            if (wantedSlot != null && !wantedSlot.equals(power.getString("Slot"))) continue;
            return true;
        }
        return false;
    }

    static ListTag readPowerList(ItemCtx ctx) {
        CustomData data = ctx.stack().get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        if (!tag.contains("Powers", Tag.TAG_LIST)) return null;
        ListTag list = tag.getList("Powers", Tag.TAG_COMPOUND);
        return list.isEmpty() ? null : list;
    }
}
