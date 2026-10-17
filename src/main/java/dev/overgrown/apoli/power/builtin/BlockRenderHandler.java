package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class BlockRenderHandler {
    private BlockRenderHandler() {}

    public static boolean hasNone(@Nullable Player player, ResourceLocation typeId) {
        if (player == null) return true;
        PowerContainer container = PowerContainer.of(player);
        return container == null || container.isEmpty() || container.powersOfType(typeId).isEmpty();
    }

    public static boolean preventsSelection(@Nullable Player player, Level level, BlockPos pos) {
        if (hasNone(player, ApoliIds.PREVENT_BLOCK_SELECTION)) return false;
        return PowerLookup.anyActive(player, ApoliIds.PREVENT_BLOCK_SELECTION, PreventBlockSelectionPower.Config.class,
            BlockRenderHandler::blocksSelection, new BlockCtx(pos, level.getBlockState(pos), level));
    }

    private static boolean blocksSelection(PreventBlockSelectionPower.Config config, BlockCtx block) {
        return config.blockCondition().isEmpty() || config.blockCondition().get().test(block);
    }
}
