package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PreventItemUseHandler {
    private PreventItemUseHandler() {}

    public static boolean isBlocked(Player user, ItemStack stack, Level level) {
        if (stack.isEmpty()) return false;
        ItemCtx ctx = new ItemCtx(stack, level, user);
        boolean[] block = new boolean[]{false};
        PowerLookup.forEach(user, Apoli.id("prevent_item_use"), PreventItemUsePower.Config.class, cfg -> {
            if (block[0]) return;
            if (cfg.itemCondition().isEmpty() || cfg.itemCondition().get().test(ctx)) {
                block[0] = true;
            }
        });
        return block[0];
    }
}
