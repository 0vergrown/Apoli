package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PreventSleepHandler {
    private PreventSleepHandler() {}

    public record Decision(BlockPos pos, Component message, boolean setsSpawn) {}

    public static @Nullable Decision decide(Player player, BlockPos pos) {
        Level level = player.level();
        BlockPos bed = pos.immutable();
        BlockCtx ctx = new BlockCtx(bed, level.getBlockState(bed), level, player);
        PreventSleepPower.Config[] best = new PreventSleepPower.Config[]{null};
        boolean[] setsSpawn = {true};
        PowerLookup.forEach(player, ApoliIds.PREVENT_SLEEP, PreventSleepPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            if (!cfg.setSpawnPoint()) setsSpawn[0] = false;
            if (best[0] == null || cfg.priority() > best[0].priority()) best[0] = cfg;
        });
        if (best[0] == null) return null;
        return new Decision(bed, best[0].message(), setsSpawn[0]);
    }
}
