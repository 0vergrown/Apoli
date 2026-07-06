package dev.overgrown.apoli.compat.icarus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cammiescorner.icarus.client.IcarusClient;
import dev.overgrown.apoli.compat.icarus.IcarusFieldHooks;
import dev.overgrown.apoli.compat.icarus.WingsAccess;
import dev.overgrown.apoli.compat.icarus.WingsPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = IcarusClient.class, remap = false)
public class IcarusClientMixin {

    @ModifyReturnValue(method = "getWingsForRendering", at = @At("RETURN"))
    private static ItemStack apoli$renderWings(ItemStack original, LivingEntity entity) {
        IcarusFieldHooks.install();
        if (original.isEmpty()) {
            WingsPower.Config cfg = WingsAccess.get(entity);
            if (cfg != null) return cfg.wingsType();
        }
        return original;
    }
}
