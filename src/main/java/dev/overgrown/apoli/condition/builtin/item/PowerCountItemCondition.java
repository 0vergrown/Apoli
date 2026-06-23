package dev.overgrown.apoli.condition.builtin.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

public final class PowerCountItemCondition implements ConditionType<ItemCtx, PowerCountItemCondition.Cfg> {
    public record Cfg(Optional<EquipmentSlot> slot, Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EquipmentSlot.CODEC.optionalFieldOf("slot").forGetter(Cfg::slot),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, ItemCtx ctx) {
        ListTag powers = HasPowerItemCondition.readPowerList(ctx);
        int count;
        if (powers == null) {
            count = 0;
        } else if (cfg.slot.isEmpty()) {
            count = powers.size();
        } else {
            String wantedSlot = cfg.slot.get().getSerializedName();
            count = 0;
            for (Tag entry : powers) {
                if (entry instanceof CompoundTag power && wantedSlot.equals(power.getString("Slot"))) {
                    count++;
                }
            }
        }
        return cfg.comparison.compare(count, cfg.compareTo);
    }
}
