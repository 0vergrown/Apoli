package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerEatMixin {

    @WrapOperation(
        method = "eat",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V"))
    private void apoli$modifyFoodValues(FoodData data, Item item, ItemStack stack, Operation<Void> original) {
        if (!item.isEdible()) {
            original.call(data, item, stack);
            return;
        }
        FoodProperties food = item.getFoodProperties();
        if (food == null) {
            original.call(data, item, stack);
            return;
        }
        ModifyFoodHandler.Values values = ModifyFoodHandler.values(
            (Player) (Object) this, stack, food.getNutrition(), food.getSaturationModifier());
        if (values == null) {
            original.call(data, item, stack);
            return;
        }
        data.eat(values.nutrition(), values.saturation());
    }
}
