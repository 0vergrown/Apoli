package dev.overgrown.apoli.mixin.reach;

import dev.overgrown.apoli.attribute.ApoliAttributes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererReachMixin {

    @ModifyConstant(method = "pick(F)V", constant = @Constant(doubleValue = 3.0))
    private double apoli$entityPickRange(double original) {
        return original + apoli$bonus();
    }

    @ModifyConstant(method = "pick(F)V", constant = @Constant(doubleValue = 9.0))
    private double apoli$entityPickRangeSquared(double original) {
        return ApoliAttributes.extendSquared(original, apoli$bonus());
    }

    private static double apoli$bonus() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0.0 : ApoliAttributes.entityInteractionBonus(player);
    }
}
