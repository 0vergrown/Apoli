package dev.overgrown.apoli.gui;

import java.util.Arrays;
import java.util.List;

public final class GuiLayout {
    public static final int INVENTORY_WIDTH = 176;
    public static final int SLOT_SIZE = 18;

    private final List<GuiElement> elements;
    private final int[] slots;
    private final int[] sliders;
    private final int[] buttons;
    private final int[] progressImages;
    private final int[] trackedEntities;
    private final int[] ordinals;
    private final int words;
    private final int progressBase;
    private final int sliderBase;
    private final int entityBase;
    private final int dataCount;

    public GuiLayout(List<GuiElement> elements) {
        this.elements = List.copyOf(elements);
        int n = this.elements.size();
        int[] slotScratch = new int[n];
        int[] sliderScratch = new int[n];
        int[] buttonScratch = new int[n];
        int[] progressScratch = new int[n];
        int[] entityScratch = new int[n];
        int slotCount = 0;
        int sliderCount = 0;
        int buttonCount = 0;
        int progressCount = 0;
        int entityCount = 0;
        this.ordinals = new int[n];
        Arrays.fill(this.ordinals, -1);
        for (int i = 0; i < n; i++) {
            GuiElement element = this.elements.get(i);
            if (element instanceof GuiElement.SlotView) {
                ordinals[i] = slotCount;
                slotScratch[slotCount++] = i;
            } else if (element instanceof GuiElement.Slider) {
                ordinals[i] = sliderCount;
                sliderScratch[sliderCount++] = i;
            } else if (element instanceof GuiElement.Button) {
                ordinals[i] = buttonCount;
                buttonScratch[buttonCount++] = i;
            } else if (element instanceof GuiElement.Image image && image.progress().isPresent()) {
                ordinals[i] = progressCount;
                progressScratch[progressCount++] = i;
            } else if (element instanceof GuiElement.EntityView view && view.tracksWorldEntity()) {
                ordinals[i] = entityCount;
                entityScratch[entityCount++] = i;
            }
        }
        this.slots = Arrays.copyOf(slotScratch, slotCount);
        this.sliders = Arrays.copyOf(sliderScratch, sliderCount);
        this.buttons = Arrays.copyOf(buttonScratch, buttonCount);
        this.progressImages = Arrays.copyOf(progressScratch, progressCount);
        this.trackedEntities = Arrays.copyOf(entityScratch, entityCount);
        this.words = (n + 15) / 16;
        this.progressBase = words * 2;
        this.sliderBase = progressBase + progressCount;
        this.entityBase = sliderBase + sliderCount;
        this.dataCount = entityBase + entityCount * 2;
    }

    public List<GuiElement> elements() {
        return elements;
    }

    public int size() {
        return elements.size();
    }

    public GuiElement element(int index) {
        return elements.get(index);
    }

    public int ordinal(int element) {
        return ordinals[element];
    }

    public int slotCount() {
        return slots.length;
    }

    public int slotElement(int ordinal) {
        return slots[ordinal];
    }

    public int sliderCount() {
        return sliders.length;
    }

    public int sliderElement(int ordinal) {
        return sliders[ordinal];
    }

    public int sliderOrdinal(String id) {
        for (int k = 0; k < sliders.length; k++) {
            if (((GuiElement.Slider) elements.get(sliders[k])).id().equals(id)) return k;
        }
        return -1;
    }

    public int buttonCount() {
        return buttons.length;
    }

    public int buttonElement(int ordinal) {
        return buttons[ordinal];
    }

    public int progressCount() {
        return progressImages.length;
    }

    public int progressElement(int ordinal) {
        return progressImages[ordinal];
    }

    public int entityCount() {
        return trackedEntities.length;
    }

    public int entityElement(int ordinal) {
        return trackedEntities[ordinal];
    }

    public int words() {
        return words;
    }

    public int visibleIndex(int element) {
        return element >> 4;
    }

    public int enabledIndex(int element) {
        return words + (element >> 4);
    }

    public int progressIndex(int ordinal) {
        return progressBase + ordinal;
    }

    public int sliderIndex(int ordinal) {
        return sliderBase + ordinal;
    }

    public int entityIndex(int ordinal) {
        return entityBase + ordinal * 2;
    }

    public int dataCount() {
        return dataCount;
    }
}
