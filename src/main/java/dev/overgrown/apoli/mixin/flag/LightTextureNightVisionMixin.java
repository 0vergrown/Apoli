package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.render.LocalNightVision;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
@OnlyIn(Dist.CLIENT)
public abstract class LightTextureNightVisionMixin {

    @ModifyExpressionValue(method = "updateLightTexture",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
            ordinal = 0))
    private boolean apoli$forceNightVision(boolean original) {
        return original || LocalNightVision.strength(Minecraft.getInstance().player) > 0f;
    }
}
