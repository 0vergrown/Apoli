package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockBreakHandler {
    private BlockBreakHandler() {}

    public static void fire(Player player, Level level, BlockPos pos, BlockState brokenState, boolean harvested) {
        BlockCtx blockCtx = new BlockCtx(pos, brokenState, level);
        PowerLookup.forEach(player, ApoliIds.ACTION_ON_BLOCK_BREAK, ActionOnBlockBreakPower.Config.class, cfg -> {
            if (cfg.onlyWhenHarvested() && !harvested) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(blockCtx)) return;
            cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, level)));
            cfg.blockAction().ifPresent(action -> action.run(blockCtx));
        });
    }
}
