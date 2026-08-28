package dev.overgrown.apoli.mixin.flag;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoodData.class)
public abstract class FoodDataRegenMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private boolean apoli$gateRegen(Player player, float amount) {
        return !PowerLookup.hasActive(player, ApoliIds.DISABLE_REGEN);
    }
}
