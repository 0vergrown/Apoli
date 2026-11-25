package dev.overgrown.apoli.mixin.reach;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererReachMixin {

    @ModifyExpressionValue(method = "pick(F)V", at = @At(value = "CONSTANT", args = "doubleValue=3.0"))
    private double apoli$entityPickRange(double original) {
        return original + apoli$bonus();
    }

    @ModifyExpressionValue(method = "pick(F)V", at = @At(value = "CONSTANT", args = "doubleValue=9.0"))
    private double apoli$entityPickRangeSquared(double original) {
        return ApoliAttributes.extendSquared(original, apoli$bonus());
    }

    private static double apoli$bonus() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0.0 : ApoliAttributes.entityInteractionBonus(player);
    }
}
