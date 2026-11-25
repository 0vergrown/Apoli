package dev.overgrown.apoli.compat.chloride.mixin;

import dev.overgrown.apoli.client.render.LocalNightVision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.srrapero720.chloride.impl.Darkness", remap = false)
@Environment(EnvType.CLIENT)
public abstract class ChlorideDarknessMixin {

    @Shadow
    public static boolean enabled;

    @Inject(method = "updateLuminance", at = @At("RETURN"), remap = false)
    private static void apoli$liftDarknessForNightVision(CallbackInfo ci) {
        if (enabled && LocalNightVision.strength(Minecraft.getInstance().player) > 0f) enabled = false;
    }
}
