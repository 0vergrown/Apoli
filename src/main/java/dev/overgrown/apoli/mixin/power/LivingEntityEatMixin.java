package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEatMixin {

    @ModifyVariable(
        method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"),
        argsOnly = true)
    private FoodProperties apoli$modifyFood(FoodProperties properties, Level level, ItemStack stack) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) return properties;
        return ModifyFoodHandler.modify(self, stack, properties);
    }

    @Inject(
        method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"))
    private void apoli$afterEat(Level level, ItemStack stack, FoodProperties properties,
                                CallbackInfoReturnable<ItemStack> cir) {
        ModifyFoodHandler.afterEat((LivingEntity) (Object) this, stack);
    }
}
