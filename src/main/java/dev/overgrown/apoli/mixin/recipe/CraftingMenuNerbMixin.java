package dev.overgrown.apoli.mixin.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overgrown.apoli.compat.ModCompat;
import dev.overgrown.apoli.recipe.ApoliPowerRecipes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuNerbMixin {

    @WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/world/level/Level;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/Recipe;)Z"))
    private static boolean apoli$allowPowerRecipeWithoutRecipeBook(ResultContainer container, Level level,
                                                                   ServerPlayer player, Recipe<?> holder,
                                                                   Operation<Boolean> original) {
        if (original.call(container, level, player, holder)) return true;
        if (!ModCompat.NERB) return false;
        if (ApoliPowerRecipes.powerFor(holder.getId()) == null) return false;
        container.setRecipeUsed(holder);
        return true;
    }
}
