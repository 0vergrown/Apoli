package dev.overgrown.apoli.compat.accessory.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.compat.accessory.Accessories;
import dev.overgrown.apoli.compat.accessory.AccessorySlot;
import dev.overgrown.apoli.compat.accessory.AccessorySlotRef;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class ModifyAccessoryAction implements ActionType<EntityCtx, ModifyAccessoryAction.Cfg> {
    public record Cfg(
        List<AccessorySlot> slots,
        Optional<ItemCondition> itemCondition,
        Optional<ItemAction> itemAction,
        Optional<EntityAction> entityAction,
        int limit,
        boolean unequip
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            AccessorySlot.LIST.optionalFieldOf("slots", List.of()).forGetter(Cfg::slots),
            ItemCondition.CODEC.optionalFieldOf("item_condition").forGetter(Cfg::itemCondition),
            ItemAction.CODEC.optionalFieldOf("item_action").forGetter(Cfg::itemAction),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Cfg::entityAction),
            Codec.INT.optionalFieldOf("limit", 0).forGetter(Cfg::limit),
            Codec.BOOL.optionalFieldOf("unequip", false).forGetter(Cfg::unequip)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity e = ctx.living();
        if (e == null) return;
        int processed = 0;

        for (AccessorySlotRef ref : Accessories.equipped(e, cfg.slots())) {
            ItemStack stack = ref.getStack();
            ItemCtx itemCtx = new ItemCtx(stack, ctx.level(), e);
            if (cfg.itemCondition().isPresent() && !cfg.itemCondition().get().test(itemCtx)) continue;
            cfg.entityAction().ifPresent(a -> a.run(ctx));
            cfg.itemAction().ifPresent(a -> a.run(itemCtx));
            if (cfg.unequip()) ref.setStack(ItemStack.EMPTY);
            if (cfg.limit() > 0 && ++processed >= cfg.limit()) break;
        }
    }
}
