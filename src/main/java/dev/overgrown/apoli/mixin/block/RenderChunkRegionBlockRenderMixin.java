package dev.overgrown.apoli.mixin.block;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.builtin.BlockRenderHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunkRegion.class)
@Environment(EnvType.CLIENT)
public abstract class RenderChunkRegionBlockRenderMixin {

    @Inject(method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD"), cancellable = true)
    private void apoli$modifyBlockRender(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null
            || BlockRenderHandler.hasNone(client.player, ApoliIds.MODIFY_BLOCK_RENDER)) {
            return;
        }
        BlockState replacement = BlockRenderHandler.replacement(client.player, client.level, pos);
        if (replacement != null) cir.setReturnValue(replacement);
    }
}
