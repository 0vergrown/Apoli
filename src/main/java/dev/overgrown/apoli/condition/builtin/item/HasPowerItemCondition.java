package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class HasPowerItemCondition implements ConditionType<ItemCtx, HasPowerItemCondition.Cfg> {
    public record Cfg(ResourceLocation power, Optional<EquipmentSlot> slot) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("power").forGetter(Cfg::power),
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
        CompoundTag tag = ctx.stack().getTag();
        if (tag == null || !tag.contains("Powers", Tag.TAG_LIST)) return null;
        ListTag list = tag.getList("Powers", Tag.TAG_COMPOUND);
        return list.isEmpty() ? null : list;
    }
}
