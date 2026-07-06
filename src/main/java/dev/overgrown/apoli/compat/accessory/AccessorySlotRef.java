package dev.overgrown.apoli.compat.accessory;

import net.minecraft.world.item.ItemStack;


public interface AccessorySlotRef {

    
    String provider();

    
    String group();

    
    String type();

    
    int index();

    
    ItemStack getStack();

    
    void setStack(ItemStack stack);

    
    default String slotId() {
        String g = group();
        return (g == null || g.isEmpty()) ? type() + "/" + index() : g + "/" + type() + "/" + index();
    }
}
