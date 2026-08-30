package dev.overgrown.apoli.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.BlockRenderRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderChunkRegion.class)
@OnlyIn(Dist.CLIENT)
public abstract class RenderChunkRegionBlockRenderMixin {

    @ModifyReturnValue(method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("RETURN"))
    private BlockState apoli$modifyBlockRender(BlockState original, BlockPos pos) {
        if (!BlockRenderRules.active()) return original;
        Level level = Minecraft.getInstance().level;
        if (level == null) return original;
        return BlockRenderRules.replace(level, pos, original);
    }
}
