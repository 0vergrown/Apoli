package dev.overgrown.apoli.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.client.render.BlockRenderRules;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldSlice.class)
@Environment(EnvType.CLIENT)
public abstract class WorldSliceBlockRenderMixin {

    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("RETURN"), require = 0)
    private BlockState apoli$modifyBlockRender(BlockState original, int x, int y, int z) {
        if (!BlockRenderRules.active()) return original;
        Level level = Minecraft.getInstance().level;
        if (level == null) return original;
        return BlockRenderRules.replace(level, new BlockPos(x, y, z), original);
    }
}
