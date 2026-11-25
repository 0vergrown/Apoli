package dev.overgrown.apoli.compat.fastbench.mixin;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.ModifyCraftingHandler;
import dev.shadowsoffire.fastbench.util.CraftResultSlotExt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftResultSlotExt.class, remap = false)
public abstract class CraftResultSlotExtMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void apoli$afterCraft(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide() || !PowerLookup.holdsType(player, ApoliIds.MODIFY_CRAFTING)) return;
        Slot self = (Slot) (Object) this;
        RecipeHolder<?> used = self.container instanceof RecipeCraftingHolder holder ? holder.getRecipeUsed() : null;
        ItemStack crafted = stack.isEmpty() ? self.container.getItem(0).copy() : stack;
        ModifyCraftingHandler.afterCraft(player, crafted, used == null ? null : used.id());
    }
}
