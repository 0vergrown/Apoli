package dev.overgrown.apoli.mixin.power;

import dev.overgrown.apoli.access.ItemPowerSlots;
import dev.overgrown.apoli.item.ItemPowerHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityItemPowerMixin implements ItemPowerSlots {

    @Unique private ItemStack[] apoli$itemPowerStacks;

    @Override
    public ItemStack[] apoli$itemPowerStacks() {
        ItemStack[] stacks = apoli$itemPowerStacks;
        if (stacks == null) {
            stacks = ItemPowerHandler.newSlotArray();
            apoli$itemPowerStacks = stacks;
        }
        return stacks;
    }
}
