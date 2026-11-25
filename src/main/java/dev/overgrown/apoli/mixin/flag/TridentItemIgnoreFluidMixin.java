package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.IgnoreFluidPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TridentItem.class)
public abstract class TridentItemIgnoreFluidMixin {

    @ModifyExpressionValue(method = {"use", "releaseUsing"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean apoli$riptideInIgnoredWater(boolean original, @Local Player player) {
        return original || IgnoreFluidPower.inIgnoredWater(player);
    }
}
