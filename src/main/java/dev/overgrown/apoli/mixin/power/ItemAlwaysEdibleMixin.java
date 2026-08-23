package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class ItemAlwaysEdibleMixin {

    @ModifyExpressionValue(
        method = "use",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodProperties;canAlwaysEat()Z"))
    private boolean apoli$modifyFoodAlwaysEdible(boolean original, Level level, Player player, InteractionHand hand) {
        if (original) return true;
        return ModifyFoodHandler.alwaysEdible(player, player.getItemInHand(hand));
    }

    @ModifyReturnValue(method = "getUseDuration", at = @At("RETURN"))
    private int apoli$modifyFoodEatTicks(int original, ItemStack stack, LivingEntity user) {
        if (original <= 0) return original;
        double modified = ModifyFoodHandler.eatTicks(user, stack, original);
        return Math.max(1, (int) Math.round(modified));
    }
}
