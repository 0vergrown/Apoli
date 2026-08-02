package dev.overgrown.apoli.mixin.recipe;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.overgrown.apoli.power.builtin.ModifyCraftingHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuResultMixin {

    @ModifyExpressionValue(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack apoli$modifyCraftingResult(ItemStack original, AbstractContainerMenu menu, Level level,
                                                        Player player, CraftingContainer craftSlots,
                                                        ResultContainer resultContainer,
                                                        @Nullable RecipeHolder<CraftingRecipe> hint) {
        RecipeHolder<?> used = resultContainer.getRecipeUsed();
        return ModifyCraftingHandler.modifyResult(player, level, used == null ? null : used.id(), original);
    }
}
