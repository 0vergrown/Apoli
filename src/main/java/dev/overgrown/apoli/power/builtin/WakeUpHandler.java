package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WakeUpHandler {
    private WakeUpHandler() {}

    public static void fire(Player player, BlockPos bedPos, boolean wakeImmediately, boolean updateSleepingPlayers) {
        Level level = player.level();
        if (level.isClientSide()) return;
        boolean fullSleep = sleptThroughNight(player, wakeImmediately, updateSleepingPlayers);
        BlockState state = level.getBlockState(bedPos);
        BlockCtx blockCtx = new BlockCtx(bedPos, state, level, player);
        PowerLookup.forEach(player, ApoliIds.ACTION_ON_WAKE_UP, ActionOnWakeUpPower.Config.class, cfg -> {
            if (cfg.requireFullSleep() && !fullSleep) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(blockCtx)) return;
            cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, level)));
            cfg.blockAction().ifPresent(action -> action.run(blockCtx));
        });
    }

    private static boolean sleptThroughNight(Player player, boolean wakeImmediately, boolean updateSleepingPlayers) {
        if (wakeImmediately) return false;
        if (!updateSleepingPlayers) return true;
        return player.isSleepingLongEnough() && player.level().isDay();
    }
}
