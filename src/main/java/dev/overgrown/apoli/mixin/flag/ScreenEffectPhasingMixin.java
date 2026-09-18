package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ScreenEffectPhasingMixin {

    @Shadow
    @Nullable
    private static Pair<BlockState, BlockPos> getOverlayBlock(Player player) {
        throw new AssertionError("@Shadow stub — replaced by the mixin processor");
    }

    @Redirect(
        method = "renderScreenEffect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;getOverlayBlock(Lnet/minecraft/world/entity/player/Player;)Lorg/apache/commons/lang3/tuple/Pair;"))
    private static Pair<BlockState, BlockPos> apoli$suppressInWallOverlayWhilePhasing(Player player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof LivingEntity living && PowerLookup.hasActive(living, ApoliIds.PHASING)) {
            return null;
        }
        return getOverlayBlock(player);
    }
}
