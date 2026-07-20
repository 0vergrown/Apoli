package dev.overgrown.apoli.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ConjuredItems {
    public static final String TAG = "apoli:conjured";
    public static final String LOCK_TAG = "apoli:conjured_locked";

    private ConjuredItems() {}

    public static void mark(ItemStack stack, boolean locked) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG, true);
        if (locked) {
            tag.putBoolean(LOCK_TAG, true);
        }
    }

    public static boolean isConjured(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG);
    }

    public static boolean isLocked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(LOCK_TAG);
    }
}
