package dev.overgrown.apoli.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.recipe.ApoliPowerRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuGateMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;"))
    private static Optional apoli$gatePowerRecipe(RecipeManager recipeManager, RecipeType type, RecipeInput input,
                                                  Level level, RecipeHolder hint, Operation<Optional> original,
                                                  AbstractContainerMenu menu, Level menuLevel, Player player) {
        Optional result = original.call(recipeManager, type, input, level, hint);
        if (result.isPresent() && result.get() instanceof RecipeHolder<?> holder) {
            ResourceLocation powerId = ApoliPowerRecipes.powerFor(holder.id());
            if (powerId != null) {
                PowerContainer c = PowerContainer.of(player);
                if (c == null || !c.hasPower(powerId) || c.isSuppressed(powerId)) {
                    return Optional.empty();
                }
            }
        }
        return result;
    }
}
