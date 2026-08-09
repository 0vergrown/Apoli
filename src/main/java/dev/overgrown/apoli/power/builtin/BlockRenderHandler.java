package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BlockRenderHandler {
    private BlockRenderHandler() {}

    public static boolean hasNone(@Nullable Player player, net.minecraft.resources.ResourceLocation typeId) {
        if (player == null) return true;
        PowerContainer container = PowerContainer.of(player);
        return container == null || container.isEmpty() || container.powersOfType(typeId).isEmpty();
    }

    public static @Nullable BlockState replacement(@Nullable Player player, Level level, BlockPos pos) {
        if (hasNone(player, ApoliIds.MODIFY_BLOCK_RENDER)) return null;
        BlockState actual = level.getBlockState(pos);
        BlockCtx ctx = new BlockCtx(pos, actual, level);
        BlockState[] out = {null};
        PowerLookup.forEach(player, ApoliIds.MODIFY_BLOCK_RENDER, ModifyBlockRenderPower.Config.class, cfg -> {
            if (out[0] != null) return;
            if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
            Block block = BuiltInRegistries.BLOCK.get(cfg.block());
            if (block != null) out[0] = block.defaultBlockState();
        });
        return out[0];
    }

    public static boolean preventsSelection(@Nullable Player player, Level level, BlockPos pos) {
        if (hasNone(player, ApoliIds.PREVENT_BLOCK_SELECTION)) return false;
        BlockCtx ctx = new BlockCtx(pos, level.getBlockState(pos), level);
        boolean[] prevented = {false};
        PowerLookup.forEach(player, ApoliIds.PREVENT_BLOCK_SELECTION, PreventBlockSelectionPower.Config.class, cfg -> {
            if (cfg.blockCondition().isEmpty() || cfg.blockCondition().get().test(ctx)) prevented[0] = true;
        });
        return prevented[0];
    }
}
