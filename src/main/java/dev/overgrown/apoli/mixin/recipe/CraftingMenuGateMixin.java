package dev.overgrown.apoli.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overgrown.apoli.recipe.ApoliPowerRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuGateMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private static Optional apoli$gatePowerRecipe(RecipeManager recipeManager, RecipeType type, Container container,
                                                  Level level, Operation<Optional> original,
                                                  AbstractContainerMenu menu, Level menuLevel, Player player) {
        Optional result = original.call(recipeManager, type, container, level);
        if (result.isPresent() && result.get() instanceof Recipe<?> recipe
            && !ApoliPowerRecipes.canCraft(player, recipe.getId())) {
            return ApoliPowerRecipes.firstCraftable(recipeManager, type, container, level, player);
        }
        return result;
    }

    @ModifyArg(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
        index = 1)
    private static ItemStack apoli$stampResultPowers(ItemStack result,
                                                     @Local(argsOnly = true) ResultContainer resultContainer) {
        Recipe<?> used = resultContainer.getRecipeUsed();
        if (!result.isEmpty() && used != null) {
            CompoundTag powers = ApoliPowerRecipes.resultPowersFor(used.getId());
            if (powers != null) result.getOrCreateTag().merge(powers);
        }
        return result;
    }
}
