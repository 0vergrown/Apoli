package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TextBar implements StringRepresentable {
    ACTIONBAR("actionbar"),
    TITLE("title"),
    SUBTITLE("subtitle"),
    TOP_LEFT("top_left"),
    TOP_CENTER("top_center"),
    TOP_RIGHT("top_right"),
    LEFT("left"),
    RIGHT("right"),
    BOTTOM_LEFT("bottom_left"),
    BOTTOM_RIGHT("bottom_right");

    public static final Codec<TextBar> CODEC = StringRepresentable.fromEnum(TextBar::values);
    public static final TextBar[] VALUES = values();

    private final String name;

    TextBar(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
