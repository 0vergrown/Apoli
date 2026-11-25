package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.render.LocalNightVision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
@Environment(EnvType.CLIENT)
public abstract class LightTextureNightVisionMixin {

    @ModifyExpressionValue(method = "updateLightTexture(F)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z",
            ordinal = 0))
    private boolean apoli$forceNightVision(boolean original) {
        return original || LocalNightVision.strength(Minecraft.getInstance().player) > 0f;
    }
}
