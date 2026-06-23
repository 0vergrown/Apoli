package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ModifyHarvestHandler {
    private ModifyHarvestHandler() {}

    public static @Nullable Boolean modify(Player player, BlockState state) {
        BlockCtx ctx = new BlockCtx(player.blockPosition(), state, player.level());
        boolean[] matched = {false};
        boolean[] allow = {false};
        PowerLookup.forEach(player, Apoli.id("modify_harvest"), ModifyHarvestPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            matched[0] = true;
            if (cfg.allow()) allow[0] = true;
        });
        return matched[0] ? allow[0] : null;
    }
}
