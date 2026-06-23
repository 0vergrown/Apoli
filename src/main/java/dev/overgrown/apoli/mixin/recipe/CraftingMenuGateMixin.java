package dev.overgrown.apoli.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.recipe.ApoliPowerRecipes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        if (result.isPresent() && result.get() instanceof Recipe<?> recipe) {
            ResourceLocation powerId = ApoliPowerRecipes.powerFor(recipe.getId());
            if (powerId != null) {
                PowerContainer c = PowerContainer.of(player);
                if (c == null || !c.hasPower(powerId) || c.isSuppressed(powerId)) {
                    return Optional.empty();
                }
            }
        }
        return result;
    }

    @Redirect(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static void apoli$stampResultPowers(ResultContainer resultContainer, int slot, ItemStack result,
                                                AbstractContainerMenu menu, Level level, Player player,
                                                CraftingContainer grid, ResultContainer rc) {
        if (!result.isEmpty() && level.getServer() != null) {
            Optional<? extends Recipe<?>> opt = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, level);
            if (opt.isPresent()) {
                CompoundTag powers = ApoliPowerRecipes.resultPowersFor(opt.get().getId());
                if (powers != null) result.getOrCreateTag().merge(powers);
            }
        }
        resultContainer.setItem(slot, result);
    }
}
