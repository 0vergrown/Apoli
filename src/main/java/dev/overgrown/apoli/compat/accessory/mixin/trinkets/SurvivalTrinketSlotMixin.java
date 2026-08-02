package dev.overgrown.apoli.compat.accessory.mixin.trinkets;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.emi.trinkets.SurvivalTrinketSlot;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import dev.overgrown.apoli.compat.accessory.backend.TrinketsBackend;
import dev.overgrown.apoli.compat.accessory.power.PreventAccessoryUnequipPower;
import dev.overgrown.apoli.item.ConjuredItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SurvivalTrinketSlot.class)
public abstract class SurvivalTrinketSlotMixin extends Slot {
    @Shadow @Final private TrinketInventory trinketInventory;
    @Shadow @Final private int slotOffset;

    public SurvivalTrinketSlotMixin(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @ModifyReturnValue(method = "mayPickup", at = @At("RETURN"))
    private boolean apoli$preventUnequip(boolean original, Player player) {
        ItemStack stack = this.getItem();
        if (ConjuredItems.isLocked(stack)) return false;
        SlotReference slotRef = new SlotReference(trinketInventory, slotOffset);
        return original && !PreventAccessoryUnequipPower.isPrevented(player, TrinketsBackend.slotRef(slotRef), stack);
    }
}
