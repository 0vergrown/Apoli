package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerEatMixin {

    @ModifyVariable(
        method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"),
        argsOnly = true)
    private FoodProperties apoli$modifyFood(FoodProperties properties, Level level, ItemStack stack) {
        return ModifyFoodHandler.modify((Player) (Object) this, stack, properties);
    }
}
