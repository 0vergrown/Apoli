package dev.overgrown.apoli.mixin.recipe;

import dev.overgrown.apoli.power.builtin.ModifyCraftingHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotCraftMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void apoli$afterCraft(Player player, ItemStack stack, CallbackInfo ci) {
        Slot self = (Slot) (Object) this;
        Recipe<?> used = self.container instanceof RecipeHolder holder ? holder.getRecipeUsed() : null;
        ModifyCraftingHandler.afterCraft(player, stack, used == null ? null : used.getId());
    }
}
