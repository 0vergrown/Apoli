package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EquippedItemAction implements ActionType<EntityCtx, EquippedItemAction.Cfg> {
    public record Cfg(EquipmentSlot equipmentSlot, ItemAction action) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.<Cfg>wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                EquipmentSlot.CODEC.fieldOf("equipment_slot").forGetter(Cfg::equipmentSlot),
                ItemAction.CODEC.fieldOf("action").forGetter(Cfg::action)
            ).apply(i, Cfg::new)),
            java.util.Map.of("item_action", "action")
        );
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.entity();
        ItemStack stack = e.getItemBySlot(cfg.equipmentSlot.vanilla());
        cfg.action.run(new ItemCtx(stack, ctx.level(), e));
    }
}
