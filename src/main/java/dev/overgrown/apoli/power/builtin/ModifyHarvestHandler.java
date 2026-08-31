package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ModifyHarvestHandler {
    private ModifyHarvestHandler() {}

    public static @Nullable Boolean modify(Player player, BlockState state) {
        if (!PowerLookup.hasActive(player, ApoliIds.MODIFY_HARVEST)) return null;
        BlockCtx ctx = new BlockCtx(player.blockPosition(), state, player.level());
        boolean[] matched = {false};
        boolean[] allow = {false};
        PowerLookup.forEach(player, ApoliIds.MODIFY_HARVEST, ModifyHarvestPower.Config.class, cfg -> {
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            matched[0] = true;
            if (cfg.allow()) allow[0] = true;
        });
        return matched[0] ? allow[0] : null;
    }

    public static ItemStack tool(Player player, BlockPos pos, BlockState state, ItemStack held) {
        if (!PowerLookup.hasActive(player, ApoliIds.MODIFY_HARVEST)) return held;
        BlockCtx ctx = new BlockCtx(pos, state, player.level());
        ItemStack[] tool = {null};
        PowerLookup.forEach(player, ApoliIds.MODIFY_HARVEST, ModifyHarvestPower.Config.class, cfg -> {
            if (tool[0] != null || cfg.stack().isEmpty()) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            tool[0] = cfg.stack().get().stack().copy();
        });
        return tool[0] != null ? tool[0] : held;
    }
}
