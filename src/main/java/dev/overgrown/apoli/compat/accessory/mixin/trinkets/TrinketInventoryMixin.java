package dev.overgrown.apoli.compat.accessory.mixin.trinkets;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.emi.trinkets.api.TrinketInventory;
import dev.overgrown.apoli.compat.accessory.backend.TrinketsBackend;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(TrinketInventory.class)
public abstract class TrinketInventoryMixin {

    @WrapMethod(method = "setItem")
    private void apoli$onSetItem(int slot, ItemStack stack, Operation<Void> original) {
        TrinketInventory self = (TrinketInventory) (Object) this;
        ItemStack old = self.getItem(slot).copy();
        original.call(slot, stack);
        if (!ItemStack.matches(old, stack)) {
            TrinketsBackend.handleChange(self, slot, old, stack);
        }
    }
}
