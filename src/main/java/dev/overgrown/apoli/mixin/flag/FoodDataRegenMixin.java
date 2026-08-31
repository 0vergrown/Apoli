package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoodData.class)
public abstract class FoodDataRegenMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean apoli$gateRegen(boolean naturalRegeneration, Player player) {
        return naturalRegeneration && !PowerLookup.hasActive(player, ApoliIds.DISABLE_REGEN);
    }
}
