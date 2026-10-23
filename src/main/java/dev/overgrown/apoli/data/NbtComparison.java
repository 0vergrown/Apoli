package dev.overgrown.apoli.data;

import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

public final class NbtComparison {
    private NbtComparison() {}

    public static boolean matches(Tag expected, Tag actual) {
        if (expected == actual) return true;
        if (expected == null) return true;
        if (actual == null) return false;
        if (expected instanceof CompoundTag expectedCompound) {
            if (!(actual instanceof CompoundTag actualCompound)) return false;
            for (String key : expectedCompound.getAllKeys()) {
                if (!matches(expectedCompound.get(key), actualCompound.get(key))) return false;
            }
            return true;
        }
        if (expected instanceof ListTag expectedList) {
            if (!(actual instanceof ListTag actualList)) return false;
            if (expectedList.isEmpty()) return actualList.isEmpty();
            for (int i = 0; i < expectedList.size(); i++) {
                Tag expectedElement = expectedList.get(i);
                boolean found = false;
                for (int j = 0; j < actualList.size(); j++) {
                    if (matches(expectedElement, actualList.get(j))) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        if (expected instanceof NumericTag expectedNum) {
            if (!(actual instanceof NumericTag actualNum)) return false;
            if (isIntegral(expected) && isIntegral(actual)) {
                return expectedNum.getAsLong() == actualNum.getAsLong();
            }
            return expectedNum.getAsDouble() == actualNum.getAsDouble();
        }
        return expected.equals(actual);
    }

    public static boolean sameValue(Tag a, Tag b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof CompoundTag aCompound) {
            if (!(b instanceof CompoundTag bCompound) || aCompound.size() != bCompound.size()) return false;
            for (String key : aCompound.getAllKeys()) {
                if (!sameValue(aCompound.get(key), bCompound.get(key))) return false;
            }
            return true;
        }
        if (a instanceof CollectionTag<?> aList) {
            if (!(b instanceof CollectionTag<?> bList) || aList.size() != bList.size()) return false;
            for (int i = 0; i < aList.size(); i++) {
                if (!sameValue(aList.get(i), bList.get(i))) return false;
            }
            return true;
        }
        if (a instanceof NumericTag aNum) {
            if (!(b instanceof NumericTag bNum)) return false;
            if (isIntegral(a) && isIntegral(b)) return aNum.getAsLong() == bNum.getAsLong();
            return aNum.getAsDouble() == bNum.getAsDouble();
        }
        return a.equals(b);
    }

    private static boolean isIntegral(Tag tag) {
        byte id = tag.getId();
        return id == Tag.TAG_BYTE || id == Tag.TAG_SHORT || id == Tag.TAG_INT || id == Tag.TAG_LONG;
    }
}
