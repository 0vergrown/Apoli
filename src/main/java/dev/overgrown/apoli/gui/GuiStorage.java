package dev.overgrown.apoli.gui;

import dev.overgrown.apoli.power.builtin.GuiPower;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class GuiStorage {
    private static final String SLIDERS = "sliders";

    private GuiStorage() {}

    public record State(SimpleContainer items, double[] sliders) {}

    public static State load(@Nullable CompoundTag stored, GuiPower.Config cfg, HolderLookup.Provider registries) {
        GuiLayout layout = cfg.layout();
        int slots = layout.slotCount();
        SimpleContainer items = new SimpleContainer(slots);
        double[] sliders = new double[layout.sliderCount()];
        for (int k = 0; k < sliders.length; k++) {
            sliders[k] = ((GuiElement.Slider) layout.element(layout.sliderElement(k))).start();
        }
        if (stored == null) return new State(items, sliders);
        NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(stored, list, registries);
        for (int i = 0; i < slots; i++) items.setItem(i, list.get(i));
        CompoundTag saved = stored.getCompound(SLIDERS);
        for (int k = 0; k < sliders.length; k++) {
            GuiElement.Slider slider = (GuiElement.Slider) layout.element(layout.sliderElement(k));
            String key = slider.storageKey(k);
            if (saved.contains(key, Tag.TAG_ANY_NUMERIC)) sliders[k] = slider.snap(saved.getDouble(key));
        }
        return new State(items, sliders);
    }

    public static CompoundTag save(SimpleContainer items, double[] sliders, GuiPower.Config cfg,
                                   HolderLookup.Provider registries) {
        GuiLayout layout = cfg.layout();
        int slots = Math.min(items.getContainerSize(), layout.slotCount());
        NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < slots; i++) list.set(i, items.getItem(i));
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, list, registries);
        CompoundTag saved = new CompoundTag();
        for (int k = 0; k < sliders.length && k < layout.sliderCount(); k++) {
            GuiElement.Slider slider = (GuiElement.Slider) layout.element(layout.sliderElement(k));
            saved.putDouble(slider.storageKey(k), sliders[k]);
        }
        tag.put(SLIDERS, saved);
        return tag;
    }

    public static double slider(@Nullable CompoundTag stored, GuiElement.Slider slider, int ordinal) {
        if (stored != null) {
            CompoundTag saved = stored.getCompound(SLIDERS);
            String key = slider.storageKey(ordinal);
            if (saved.contains(key, Tag.TAG_ANY_NUMERIC)) return slider.snap(saved.getDouble(key));
        }
        return slider.start();
    }
}
