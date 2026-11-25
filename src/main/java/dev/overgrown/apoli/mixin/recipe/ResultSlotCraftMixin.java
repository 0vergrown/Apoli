package dev.overgrown.apoli.mixin.recipe;

import dev.overgrown.apoli.power.builtin.ModifyCraftingHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotCraftMixin {

    @Unique
    private ItemStack apoli$quickCrafted = ItemStack.EMPTY;

    @Inject(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"))
    private void apoli$rememberQuickCraft(ItemStack stack, int amount, CallbackInfo ci) {
        apoli$quickCrafted = stack;
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void apoli$afterCraft(Player player, ItemStack stack, CallbackInfo ci) {
        Slot self = (Slot) (Object) this;
        ItemStack quick = apoli$quickCrafted;
        apoli$quickCrafted = ItemStack.EMPTY;
        ItemStack crafted = stack.isEmpty() ? quick : stack;
        RecipeHolder<?> used = self.container instanceof RecipeCraftingHolder holder ? holder.getRecipeUsed() : null;
        ModifyCraftingHandler.afterCraft(player, crafted, used == null ? null : used.id());
    }
}
