package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyHarvestHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Block.class)
public abstract class BlockHarvestToolMixin {
    @ModifyVariable(method = "playerDestroy", at = @At("HEAD"), argsOnly = true)
    private ItemStack apoli$harvestTool(ItemStack tool, Level level, Player player, BlockPos pos, BlockState state,
                                        BlockEntity blockEntity, ItemStack original) {
        return ModifyHarvestHandler.tool(player, pos, state, tool);
    }
}
