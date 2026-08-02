package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class ConjureEquipmentAction implements ActionType<EntityCtx, ConjureEquipmentAction.Cfg> {

    private static final Codec<EquipmentSlot> SLOT_CODEC = Codec.STRING.comapFlatMap(
        ConjureEquipmentAction::slotByName, EquipmentSlot::getSerializedName);

    public record Cfg(Optional<EquipmentSlot> slot, List<AccessorySlot> accessorySlots,
                      ItemStackData item, boolean lock) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        SLOT_CODEC.optionalFieldOf("slot").forGetter(Cfg::slot),
        AccessorySlot.LIST.optionalFieldOf("accessory_slot", List.of()).forGetter(Cfg::accessorySlots),
        ItemStackData.CODEC.fieldOf("item").forGetter(Cfg::item),
        Codec.BOOL.optionalFieldOf("lock", false).forGetter(Cfg::lock)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return INNER.flatXmap(ConjureEquipmentAction::require, DataResult::success);
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null || entity.level().isClientSide()) return;

        if (cfg.slot.isPresent()) {
            ItemStack stack = cfg.item.stack().copy();
            ConjuredItems.mark(stack, cfg.lock);
            entity.setItemSlot(cfg.slot.get().vanilla(), stack);
        }

        if (!cfg.accessorySlots.isEmpty()) {
            AccessorySlotRef ref = firstFreeSlot(entity, cfg.accessorySlots);
            if (ref != null) {
                ItemStack stack = cfg.item.stack().copy();
                ConjuredItems.mark(stack, cfg.lock);
                ref.setStack(stack);
            }
        }

        if (entity instanceof ServerPlayer player) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static DataResult<Cfg> require(Cfg cfg) {
        if (cfg.slot.isEmpty() && cfg.accessorySlots.isEmpty()) {
            return DataResult.error(() -> "apoli:conjure_equipment needs 'slot' (mainhand/offhand/head/chest/legs/feet) or 'accessory_slot'");
        }
        return DataResult.success(cfg);
    }

    private static AccessorySlotRef firstFreeSlot(LivingEntity entity, List<AccessorySlot> filter) {
        List<AccessorySlotRef> refs = Accessories.slots(entity, filter);
        if (refs.isEmpty()) return null;
        for (int i = 0; i < refs.size(); i++) {
            AccessorySlotRef ref = refs.get(i);
            if (ref.getStack().isEmpty()) return ref;
        }
        return refs.get(0);
    }

    private static DataResult<EquipmentSlot> slotByName(String raw) {
        String name = raw.startsWith("slot.") ? raw.substring(5) : raw;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getSerializedName().equals(name) || slot.name().equals(name)) {
                return DataResult.success(slot);
            }
        }
        return DataResult.error(() -> "Unknown equipment slot '" + raw
            + "' (expected mainhand/offhand/head/chest/legs/feet, optionally prefixed with 'slot.')");
    }
}
