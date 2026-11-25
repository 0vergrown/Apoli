package dev.overgrown.apoli.gui;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum GuiAnchor implements StringRepresentable {
    TOP_LEFT("top_left", 0, 0),
    TOP("top", 1, 0),
    TOP_RIGHT("top_right", 2, 0),
    LEFT("left", 0, 1),
    CENTER("center", 1, 1),
    RIGHT("right", 2, 1),
    BOTTOM_LEFT("bottom_left", 0, 2),
    BOTTOM("bottom", 1, 2),
    BOTTOM_RIGHT("bottom_right", 2, 2);

    public static final Codec<GuiAnchor> CODEC = StringRepresentable.fromEnum(GuiAnchor::values);

    private final String name;
    private final int column;
    private final int row;

    GuiAnchor(String name, int column, int row) {
        this.name = name;
        this.column = column;
        this.row = row;
    }

    public int x(int width) {
        return column * width / 2;
    }

    public int y(int height) {
        return row * height / 2;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
