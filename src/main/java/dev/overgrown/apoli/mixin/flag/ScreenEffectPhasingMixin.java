package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.ApoliIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ScreenEffectPhasingMixin {

    @Shadow
    @Nullable
    private static BlockState getViewBlockingState(Player player) {
        throw new AssertionError("@Shadow stub — replaced by the mixin processor");
    }

    @Redirect(
        method = "renderScreenEffect",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;getViewBlockingState(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState apoli$suppressInWallOverlayWhilePhasing(Player player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof LivingEntity living && PowerLookup.hasActive(living, ApoliIds.PHASING)) {
            return null;
        }
        return getViewBlockingState(player);
    }
}
