package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.builtin.entity.InventoryHelper;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.InventoryType;
import dev.overgrown.apoli.data.ItemSlot;
import dev.overgrown.apoli.data.ProcessMode;
import dev.overgrown.apoli.power.builtin.InventoryPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public final class InventoryCondition implements ConditionType<EntityCtx, InventoryCondition.Cfg> {
    public record Cfg(
        Optional<List<InventoryType>> inventoryTypes,
        ProcessMode processMode,
        Optional<ItemCondition> itemCondition,
        Optional<List<ItemSlot>> slots,
        Optional<ItemSlot> slot,
        Optional<ResourceLocation> power,
        Comparison comparison,
        int compareTo
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.list(InventoryType.CODEC).optionalFieldOf("inventory_types").forGetter(Cfg::inventoryTypes),
            ProcessMode.CODEC.optionalFieldOf("process_mode", ProcessMode.ITEMS).forGetter(Cfg::processMode),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Cfg::itemCondition),
            Codec.list(ItemSlot.CODEC).optionalFieldOf("slots").forGetter(Cfg::slots),
            ItemSlot.CODEC.optionalFieldOf("slot").forGetter(Cfg::slot),
            ResourceLocation.CODEC.optionalFieldOf("power").forGetter(Cfg::power),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER).forGetter(Cfg::comparison),
            Codec.INT.optionalFieldOf("compare_to", 0).forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.entity();
        List<InventoryType> types = cfg.inventoryTypes.orElse(List.of(InventoryType.INVENTORY));
        int[] count = {0};

        if (types.contains(InventoryType.INVENTORY) && entity instanceof Player player) {
            InventoryHelper.walk(player.getInventory(), player, cfg.itemCondition, cfg.slot, cfg.slots,
                cfg.processMode, 0, (idx, stack) -> count[0]++);
        }
        if (types.contains(InventoryType.POWER) && cfg.power.isPresent()) {
            SimpleContainer container = InventoryPower.openContainer(entity, cfg.power.get());
            if (container != null) {
                InventoryHelper.walk(container, entity, cfg.itemCondition, cfg.slot, cfg.slots,
                    cfg.processMode, 0, (idx, stack) -> count[0]++);
            }
        }
        return cfg.comparison.compare(count[0], cfg.compareTo);
    }
}
