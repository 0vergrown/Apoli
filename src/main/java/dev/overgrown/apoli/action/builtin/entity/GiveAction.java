package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class GiveAction implements ActionType<EntityCtx, GiveAction.Cfg> {
    public record Cfg(
        ItemStackData stack,
        Optional<ItemAction> itemAction,
        Optional<EquipmentSlot> preferredSlot
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStackData.CODEC.fieldOf("stack").forGetter(Cfg::stack),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("item_action", ItemAction.CODEC).forGetter(Cfg::itemAction),
            EquipmentSlot.CODEC.optionalFieldOf("preferred_slot").forGetter(Cfg::preferredSlot)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return;
        ItemStack stack = cfg.stack.stack().copy();
        cfg.itemAction.ifPresent(a -> a.run(new ItemCtx(stack, ctx.level(), player)));
        if (cfg.preferredSlot.isPresent()) {
            net.minecraft.world.entity.EquipmentSlot slot = cfg.preferredSlot.get().vanilla();
            ItemStack existing = player.getItemBySlot(slot);
            if (existing.isEmpty()) {
                player.setItemSlot(slot, stack);
                return;
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
