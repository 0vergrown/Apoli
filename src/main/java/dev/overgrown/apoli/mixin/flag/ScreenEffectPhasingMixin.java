package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class ScreenEffectPhasingMixin {

    @ModifyExpressionValue(
        method = "renderScreenEffect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;getOverlayBlock(Lnet/minecraft/world/entity/player/Player;)Lorg/apache/commons/lang3/tuple/Pair;"))
    private static @Nullable Pair<BlockState, BlockPos> apoli$suppressInWallOverlayWhilePhasing(
            @Nullable Pair<BlockState, BlockPos> original) {
        if (original == null) return null;
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof LivingEntity living && PowerLookup.hasActive(living, ApoliIds.PHASING)) return null;
        return original;
    }
}
