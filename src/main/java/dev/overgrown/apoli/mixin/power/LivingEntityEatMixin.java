package dev.overgrown.apoli.mixin.power;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.builtin.EdibleItemHandler;
import dev.overgrown.apoli.power.builtin.ModifyFoodHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEatMixin {

    @WrapOperation(
        method = "startUsingItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
    private int apoli$modifyFoodEatTicks(ItemStack stack, Operation<Integer> original, InteractionHand hand) {
        int ticks = original.call(stack);
        if (ticks <= 0 || EdibleItemHandler.armedDuration(stack) >= 0) return ticks;
        double modified = ModifyFoodHandler.eatTicks((LivingEntity) (Object) this, stack, ticks);
        return Math.max(1, (int) Math.round(modified));
    }

    @Inject(method = "addEatEffect", at = @At("HEAD"), cancellable = true)
    private void apoli$preventFoodEffects(ItemStack stack, Level level, LivingEntity eater, CallbackInfo ci) {
        if (ModifyFoodHandler.preventsEffects(eater, stack)) ci.cancel();
    }

    @Inject(method = "eat", at = @At("RETURN"))
    private void apoli$afterEat(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!stack.isEdible()) return;
        ModifyFoodHandler.afterEat((LivingEntity) (Object) this, stack);
    }

    @ModifyReturnValue(method = "eat", at = @At("RETURN"))
    private ItemStack apoli$replaceEatenStack(ItemStack result, Level level, ItemStack stack) {
        if (!stack.isEdible()) return result;
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack replacement = ModifyFoodHandler.replacement(self, stack);
        if (replacement == null || replacement.isEmpty()) return result;
        if (self instanceof Player player && player.getAbilities().instabuild) return result;
        if (result.isEmpty()) return replacement;
        if (level.isClientSide()) return result;
        if (self instanceof Player player) {
            if (!player.getInventory().add(replacement)) player.drop(replacement, false);
        } else {
            self.spawnAtLocation(replacement);
        }
        return result;
    }
}
