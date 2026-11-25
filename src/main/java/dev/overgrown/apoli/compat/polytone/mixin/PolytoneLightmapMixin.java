package dev.overgrown.apoli.compat.polytone.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.client.render.LocalNightVision;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.polytone.content.lightmap.Lightmap")
@OnlyIn(Dist.CLIENT)
public abstract class PolytoneLightmapMixin {

    @ModifyExpressionValue(method = "applyToLightTexture", remap = false,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
            ordinal = 0, remap = true))
    private boolean apoli$nightVisionPower(boolean original) {
        return original || LocalNightVision.strength(Minecraft.getInstance().player) > 0f;
    }
}
