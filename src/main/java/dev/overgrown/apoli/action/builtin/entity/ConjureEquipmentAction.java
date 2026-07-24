package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ConjureEquipmentAction implements ActionType<EntityCtx, ConjureEquipmentAction.Cfg> {

    private static final Codec<EquipmentSlot> SLOT_CODEC = Codec.STRING.comapFlatMap(
        ConjureEquipmentAction::slotByName, EquipmentSlot::getSerializedName);

    public record Cfg(EquipmentSlot slot, ItemStackData item, boolean lock) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            SLOT_CODEC.fieldOf("slot").forGetter(Cfg::slot),
            ItemStackData.CODEC.fieldOf("item").forGetter(Cfg::item),
            Codec.BOOL.optionalFieldOf("lock", false).forGetter(Cfg::lock)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity entity = ctx.living();
        if (entity == null || entity.level().isClientSide()) return;
        ItemStack stack = cfg.item.stack().copy();
        ConjuredItems.mark(stack, cfg.lock);
        entity.setItemSlot(cfg.slot.vanilla(), stack);
        if (entity instanceof ServerPlayer player) {
            player.inventoryMenu.broadcastChanges();
        }
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
