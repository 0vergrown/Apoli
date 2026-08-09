package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BlockPlaceHandler {
    private BlockPlaceHandler() {}

    public static boolean isPrevented(Player player, Level level, InteractionHand hand, ItemStack stack,
                                      BlockPos toPos, BlockPos onPos, Direction direction) {
        BlockCtx toCtx = new BlockCtx(toPos, level.getBlockState(toPos), level);
        BlockCtx onCtx = new BlockCtx(onPos, level.getBlockState(onPos), level);
        boolean[] prevented = {false};
        PowerLookup.forEach(player, ApoliIds.PREVENT_BLOCK_PLACE, PreventBlockPlacePower.Config.class, cfg -> {
            if (!BlockInteractionHelper.handMatches(cfg.hands(), hand)) return;
            if (!BlockInteractionHelper.directionMatches(cfg.directions(), direction)) return;
            if (!BlockInteractionHelper.itemMatches(cfg.itemCondition(), stack, player)) return;
            if (cfg.placeToCondition().isPresent() && !cfg.placeToCondition().get().test(toCtx)) return;
            if (cfg.placeOnCondition().isPresent() && !cfg.placeOnCondition().get().test(onCtx)) return;
            prevented[0] = true;
            if (level.isClientSide()) return;
            cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, level)));
            cfg.placeToAction().ifPresent(action -> action.run(toCtx));
            cfg.placeOnAction().ifPresent(action -> action.run(onCtx));
            BlockInteractionHelper.applyItemResult(player, hand, stack,
                cfg.heldItemAction(), cfg.resultStack(), cfg.resultItemAction());
        });
        return prevented[0];
    }

    public static void fireAfterPlace(Player player, Level level, InteractionHand hand, ItemStack stack,
                                      BlockPos toPos, BlockPos onPos, Direction direction) {
        if (level.isClientSide()) return;
        BlockCtx toCtx = new BlockCtx(toPos, level.getBlockState(toPos), level);
        BlockCtx onCtx = new BlockCtx(onPos, level.getBlockState(onPos), level);
        PowerLookup.forEach(player, ApoliIds.ACTION_ON_BLOCK_PLACE, ActionOnBlockPlacePower.Config.class, cfg -> {
            if (!BlockInteractionHelper.handMatches(cfg.hands(), hand)) return;
            if (!BlockInteractionHelper.directionMatches(cfg.directions(), direction)) return;
            if (!BlockInteractionHelper.itemMatches(cfg.itemCondition(), stack, player)) return;
            if (cfg.placeToCondition().isPresent() && !cfg.placeToCondition().get().test(toCtx)) return;
            if (cfg.placeOnCondition().isPresent() && !cfg.placeOnCondition().get().test(onCtx)) return;
            cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, level)));
            cfg.placeToAction().ifPresent(action -> action.run(toCtx));
            cfg.placeOnAction().ifPresent(action -> action.run(onCtx));
            BlockInteractionHelper.applyItemResult(player, hand, stack,
                cfg.heldItemAction(), cfg.resultStack(), cfg.resultItemAction());
        });
    }
}
