package dev.overgrown.apoli.mixin.enchantment;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ModifyEnchantmentLevelHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperModifyLevelMixin {

    private static final String GET_OR_DEFAULT =
        "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;";

    private static final String ITERATE_ITEM =
        "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V";

    private static final String ITERATE_ITEM_IN_SLOT =
        "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V";

    @ModifyReturnValue(method = "getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private static int apoli$modifyItemLevel(int original, Holder<Enchantment> enchantment, ItemStack stack) {
        return ModifyEnchantmentLevelHandler.level(stack, enchantment, original);
    }

    @ModifyReturnValue(method = "getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"))
    private static int apoli$modifyEmptySlotLevel(int original, Holder<Enchantment> enchantment, LivingEntity living) {
        return ModifyEnchantmentLevelHandler.emptySlotLevel(living, enchantment, original);
    }

    @ModifyExpressionValue(method = ITERATE_ITEM, at = @At(value = "INVOKE", target = GET_OR_DEFAULT))
    private static Object apoli$modifyIteratedEnchantments(Object original, @Local(argsOnly = true) ItemStack stack) {
        return ModifyEnchantmentLevelHandler.enchantments(stack, (ItemEnchantments) original);
    }

    @ModifyExpressionValue(method = ITERATE_ITEM_IN_SLOT, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private static boolean apoli$iterateEmptySlotsOfHolders(boolean original, @Local(argsOnly = true) LivingEntity living) {
        return original && !ModifyEnchantmentLevelHandler.isHolder(living);
    }

    @ModifyExpressionValue(method = ITERATE_ITEM_IN_SLOT, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object apoli$modifySlotEnchantments(Object original, @Local(argsOnly = true) ItemStack stack,
                                                       @Local(argsOnly = true) LivingEntity living) {
        return ModifyEnchantmentLevelHandler.enchantmentsFor(living, stack, (ItemEnchantments) original);
    }

    @ModifyExpressionValue(method = "hasTag", at = @At(value = "INVOKE", target = GET_OR_DEFAULT))
    private static Object apoli$modifyTaggedEnchantments(Object original, @Local(argsOnly = true) ItemStack stack) {
        return ModifyEnchantmentLevelHandler.enchantments(stack, (ItemEnchantments) original);
    }

    @WrapOperation(method = "getRandomItemWith", at = @At(value = "INVOKE", target = GET_OR_DEFAULT))
    private static Object apoli$modifyRandomItemEnchantments(ItemStack stack, DataComponentType<?> type, Object fallback,
                                                             Operation<Object> original,
                                                             @Local(argsOnly = true) LivingEntity living) {
        return ModifyEnchantmentLevelHandler.enchantmentsFor(living, stack, (ItemEnchantments) original.call(stack, type, fallback));
    }

    @Inject(method = {"modifyDamage", "modifyFallBasedDamage", "modifyArmorEffectiveness", "modifyKnockback"}, at = @At("HEAD"))
    private static void apoli$hintAttacker(ServerLevel level, ItemStack weapon, Entity target, DamageSource source, float amount,
                                           CallbackInfoReturnable<Float> cir) {
        ModifyEnchantmentLevelHandler.hintAttacker(weapon, source);
    }
}
