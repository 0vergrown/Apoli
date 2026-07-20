package dev.overgrown.apoli.item;

import dev.overgrown.apoli.data.EquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.Set;

public final class ItemPowers {
    private ItemPowers() {}

    public static final String POWERS = "Powers";
    public static final String ID = "Id";
    public static final String SLOT = "Slot";
    public static final String HIDDEN = "Hidden";
    public static final String NEGATIVE = "Negative";
    public static final String VALUE = "Value";

    public static @Nullable ListTag read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(POWERS, Tag.TAG_LIST)) return null;
        ListTag list = tag.getList(POWERS, Tag.TAG_COMPOUND);
        return list.isEmpty() ? null : list;
    }

    public static Set<ResourceLocation> powerIdsForSlot(ItemStack stack, EquipmentSlot slot) {
        ListTag list = read(stack);
        if (list == null) return Set.of();
        String wantedSlot = slot.getSerializedName();
        Set<ResourceLocation> out = null;
        for (Tag entry : list) {
            if (!(entry instanceof CompoundTag c)) continue;
            if (!wantedSlot.equals(c.getString(SLOT))) continue;
            ResourceLocation id = ResourceLocation.tryParse(c.getString(ID));
            if (id == null) continue;
            if (out == null) out = new LinkedHashSet<>();
            out.add(id);
        }
        return out == null ? Set.of() : out;
    }

    public static void add(ItemStack stack, ResourceLocation power, EquipmentSlot slot, boolean hidden, boolean negative) {
        String id = power.toString();
        String slotName = slot.getSerializedName();
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList(POWERS, Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            if (entry instanceof CompoundTag c && id.equals(c.getString(ID)) && slotName.equals(c.getString(SLOT))) {
                return;
            }
        }
        CompoundTag e = new CompoundTag();
        e.putString(ID, id);
        e.putString(SLOT, slotName);
        if (hidden) e.putBoolean(HIDDEN, true);
        if (negative) e.putBoolean(NEGATIVE, true);
        list.add(e);
        tag.put(POWERS, list);
    }

    public static OptionalInt readValue(ItemStack stack, ResourceLocation power, EquipmentSlot slot) {
        ListTag list = read(stack);
        if (list == null) return OptionalInt.empty();
        String id = power.toString();
        String slotName = slot.getSerializedName();
        for (Tag entry : list) {
            if (entry instanceof CompoundTag c && id.equals(c.getString(ID)) && slotName.equals(c.getString(SLOT))) {
                return c.contains(VALUE, Tag.TAG_INT) ? OptionalInt.of(c.getInt(VALUE)) : OptionalInt.empty();
            }
        }
        return OptionalInt.empty();
    }

    public static void saveValue(ItemStack stack, ResourceLocation power, EquipmentSlot slot, int value) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(POWERS, Tag.TAG_LIST)) return;
        String id = power.toString();
        String slotName = slot.getSerializedName();
        ListTag list = tag.getList(POWERS, Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            if (entry instanceof CompoundTag c && id.equals(c.getString(ID)) && slotName.equals(c.getString(SLOT))) {
                c.putInt(VALUE, value);
                return;
            }
        }
    }

    public static void remove(ItemStack stack, ResourceLocation power, @Nullable EquipmentSlot slot) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(POWERS, Tag.TAG_LIST)) return;
        String id = power.toString();
        String slotName = slot == null ? null : slot.getSerializedName();
        ListTag list = tag.getList(POWERS, Tag.TAG_COMPOUND);
        list.removeIf(entry -> entry instanceof CompoundTag c
            && id.equals(c.getString(ID))
            && (slotName == null || slotName.equals(c.getString(SLOT))));
        if (list.isEmpty()) {
            tag.remove(POWERS);
            if (tag.isEmpty()) stack.setTag(null);
        } else {
            tag.put(POWERS, list);
        }
    }

    public static @Nullable EquipmentSlot slotByName(String name) {
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getSerializedName().equals(name)) return s;
        }
        return null;
    }
}
