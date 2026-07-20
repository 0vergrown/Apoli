package dev.overgrown.apoli.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ConjuredItems {
    public static final String TAG = "apoli:conjured";
    public static final String LOCK_TAG = "apoli:conjured_locked";

    private ConjuredItems() {}

    public static void mark(ItemStack stack, boolean locked) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(TAG, true);
            if (locked) {
                tag.putBoolean(LOCK_TAG, true);
            }
        });
    }

    public static boolean isConjured(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }

    public static boolean isLocked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(LOCK_TAG);
    }
}
