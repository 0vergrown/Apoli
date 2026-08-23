package dev.overgrown.apoli.mixin.reach;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeReachMixin {

    @ModifyReturnValue(method = "getPickRange", at = @At("RETURN"))
    private float apoli$blockPickRange(float original) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return original;
        double bonus = ApoliAttributes.blockInteractionRange(player) - ApoliAttributes.DEFAULT_BLOCK_INTERACTION_RANGE;
        return bonus == 0.0 ? original : (float) (original + bonus);
    }
}
