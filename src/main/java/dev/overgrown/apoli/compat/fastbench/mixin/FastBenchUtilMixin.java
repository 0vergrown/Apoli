package dev.overgrown.apoli.compat.fastbench.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.power.builtin.ModifyCraftingHandler;
import dev.overgrown.apoli.recipe.ApoliPowerRecipes;
import dev.shadowsoffire.fastbench.util.CraftingInventoryExt;
import dev.shadowsoffire.fastbench.util.FastBenchUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FastBenchUtil.class, remap = false)
public abstract class FastBenchUtilMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void apoli$dropUncraftableCachedRecipe(Level level, Player player, CraftingInventoryExt inv,
                                                         ResultContainer result, CallbackInfo ci) {
        if (level.isClientSide || !inv.checkChanges) return;
        RecipeHolder<?> cached = result.getRecipeUsed();
        if (cached == null || ApoliPowerRecipes.canCraft(player, cached.id())) return;
        result.setRecipeUsed(null);
        result.setItem(0, ItemStack.EMPTY);
    }

    @ModifyExpressionValue(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Ldev/shadowsoffire/fastbench/util/FastBenchUtil;findRecipe(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/item/crafting/RecipeHolder;"))
    private static @Nullable RecipeHolder<CraftingRecipe> apoli$gatePowerRecipe(@Nullable RecipeHolder<CraftingRecipe> found,
                                                                               Level level, Player player,
                                                                               CraftingInventoryExt inv,
                                                                               ResultContainer result) {
        if (found == null || ApoliPowerRecipes.canCraft(player, found.id())) return found;
        return ApoliPowerRecipes.firstCraftable(level.getRecipeManager(), RecipeType.CRAFTING, inv.asCraftInput(), level, player)
            .orElse(null);
    }

    @ModifyExpressionValue(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack apoli$modifyGridResult(ItemStack original, Level level, Player player,
                                                    CraftingInventoryExt inv, ResultContainer result,
                                                    @Local(ordinal = 1) RecipeHolder<CraftingRecipe> recipe) {
        return ModifyCraftingHandler.modifyResult(player, level, recipe == null ? null : recipe.id(), original);
    }

    @ModifyExpressionValue(method = "handleShiftCraft(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/inventory/Slot;Ldev/shadowsoffire/fastbench/util/CraftingInventoryExt;Lnet/minecraft/world/inventory/ResultContainer;Ldev/shadowsoffire/fastbench/util/FastBenchUtil$OutputMover;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack apoli$modifyShiftCraftResult(ItemStack original, Player player,
                                                          AbstractContainerMenu container, Slot resultSlot,
                                                          CraftingInventoryExt craftMatrix, ResultContainer craftResult,
                                                          FastBenchUtil.OutputMover mover) {
        RecipeHolder<?> used = craftResult.getRecipeUsed();
        ItemStack modified = ModifyCraftingHandler.modifyResult(player, player.level(), used == null ? null : used.id(), original);
        return modified.isEmpty() ? original : modified;
    }
}
