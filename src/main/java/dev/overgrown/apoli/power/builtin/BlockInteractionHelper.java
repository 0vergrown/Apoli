package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.action.ItemAction;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.data.Hand;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class BlockInteractionHelper {
    private BlockInteractionHelper() {}

    public static boolean handMatches(List<Hand> hands, InteractionHand hand) {
        if (hands.size() == Hand.BOTH.size()) return true;
        for (int i = 0; i < hands.size(); i++) {
            if (hands.get(i).vanilla() == hand) return true;
        }
        return false;
    }

    public static boolean directionMatches(List<PreventBlockPlacePower.Dir> dirs, Direction direction) {
        if (direction == null || dirs.size() == PreventBlockPlacePower.Dir.ALL.size()) return true;
        for (int i = 0; i < dirs.size(); i++) {
            if (dirs.get(i).vanilla() == direction) return true;
        }
        return false;
    }

    public static boolean itemMatches(Optional<ItemCondition> condition, ItemStack stack, Player player) {
        return condition.isEmpty() || condition.get().test(new ItemCtx(stack, player.level(), player));
    }

    public static void applyItemResult(Player player, InteractionHand hand, ItemStack used,
                                       Optional<ItemAction> heldItemAction,
                                       Optional<ItemStackData> resultStack,
                                       Optional<ItemAction> resultItemAction) {
        if (heldItemAction.isPresent()) {
            heldItemAction.get().run(new ItemCtx(used, player.level(), player));
        }
        if (resultStack.isEmpty()) return;
        ItemStack result = resultStack.get().stack().copy();
        if (resultItemAction.isPresent()) {
            resultItemAction.get().run(new ItemCtx(result, player.level(), player));
        }
        player.setItemInHand(hand, result);
    }
}
