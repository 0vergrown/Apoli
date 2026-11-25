package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ScreenEffectPhasingMixin {

    @ModifyExpressionValue(
        method = "renderScreenEffect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;getViewBlockingState(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static @Nullable BlockState apoli$suppressInWallOverlayWhilePhasing(@Nullable BlockState original) {
        if (original == null) return null;
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof LivingEntity living && PowerLookup.hasActive(living, ApoliIds.PHASING)) return null;
        return original;
    }
}
