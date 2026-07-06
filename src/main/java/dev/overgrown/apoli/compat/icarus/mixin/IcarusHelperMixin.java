package dev.overgrown.apoli.compat.icarus.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cammiescorner.icarus.api.IcarusPlayerValues;
import dev.cammiescorner.icarus.util.IcarusHelper;
import dev.overgrown.apoli.compat.icarus.WingsAccess;
import dev.overgrown.apoli.compat.icarus.WingsPower;
import dev.overgrown.apoli.compat.icarus.WingsValues;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = IcarusHelper.class, remap = false)
public class IcarusHelperMixin {

    @ModifyReturnValue(method = "getConfigValues", at = @At("RETURN"))
    private static IcarusPlayerValues apoli$injectWingsConfig(IcarusPlayerValues original, LivingEntity entity) {
        WingsPower.Config cfg = WingsAccess.get(entity);
        return cfg != null ? WingsValues.of(cfg, original) : original;
    }
}
