package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.WaterBreathingPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitWaterBreathingMixin {

    @ModifyExpressionValue(method = "applyEffects", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private static boolean apoli$conduitReachesWaterBreathers(boolean original, @Local Player player) {
        return original || WaterBreathingPower.suffocatesOutsideWater(player);
    }
}
