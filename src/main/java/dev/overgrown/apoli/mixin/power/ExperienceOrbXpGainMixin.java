package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyXpGainPower;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbXpGainMixin {

    @ModifyExpressionValue(method = "playerTouch", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/world/entity/player/Player;I)I"))
    private int apoli$modifyXpGain(int remaining, Player player) {
        return ModifyXpGainPower.modifyGain(player, remaining);
    }
}
