package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.ActionResult;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockUseHandler {
    private BlockUseHandler() {}

    public static InteractionResult handle(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        BlockCtx blockCtx = new BlockCtx(pos, state, level, player);

        if (isPrevented(player, blockCtx)) {
            return InteractionResult.FAIL;
        }
        return runActions(player, level, hand, hit, blockCtx);
    }

    private static boolean isPrevented(Player player, BlockCtx blockCtx) {
        boolean[] prevented = {false};
        PowerLookup.forEach(player, ApoliIds.PREVENT_BLOCK_USE, PreventBlockUsePower.Config.class, cfg -> {
            if (cfg.blockCondition().test(blockCtx)) prevented[0] = true;
        });
        return prevented[0];
    }

    private static InteractionResult runActions(Player player, Level level, InteractionHand hand,
                                                BlockHitResult hit, BlockCtx blockCtx) {
        ItemStack stack = player.getItemInHand(hand);
        ActionResult[] result = {null};
        PowerLookup.forEach(player, ApoliIds.ACTION_ON_BLOCK_USE, ActionOnBlockUsePower.Config.class, cfg -> {
            if (!BlockInteractionHelper.handMatches(cfg.hands(), hand)) return;
            if (!BlockInteractionHelper.directionMatches(cfg.directions(), hit.getDirection())) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(blockCtx)) return;
            if (!BlockInteractionHelper.itemMatches(cfg.itemCondition(), stack, player)) return;

            if (result[0] == null || cfg.actionResult() != ActionResult.PASS) {
                result[0] = cfg.actionResult();
            }
            if (level.isClientSide()) return;

            cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, level)));
            cfg.blockAction().ifPresent(action -> action.run(blockCtx));
            BlockInteractionHelper.applyItemResult(player, hand, stack,
                cfg.heldItemAction(), cfg.resultStack(), cfg.resultItemAction());
        });
        return result[0] == null ? InteractionResult.PASS : result[0].vanilla();
    }
}
