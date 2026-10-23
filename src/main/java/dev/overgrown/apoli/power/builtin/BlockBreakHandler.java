package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class BlockBreakHandler {
    private BlockBreakHandler() {}

    private static final List<ResourceLocation> RUNNING = new ArrayList<>(2);

    public static void fire(Player player, Level level, BlockPos pos, BlockState brokenState, boolean harvested) {
        BlockCtx blockCtx = new BlockCtx(pos, brokenState, level, player);
        PowerLookup.forEachEntry(player, ApoliIds.ACTION_ON_BLOCK_BREAK, ActionOnBlockBreakPower.Config.class, (powerId, cfg) -> {
            if (cfg.onlyWhenHarvested() && !harvested) return;
            if (RUNNING.contains(powerId)) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(blockCtx)) return;
            RUNNING.add(powerId);
            try {
                cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, level)));
                cfg.blockAction().ifPresent(action -> action.run(blockCtx));
            } finally {
                RUNNING.remove(RUNNING.size() - 1);
            }
        });
    }
}
