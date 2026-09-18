package dev.overgrown.apoli.data;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ItemNames {

    private ItemNames() {}

    public static Component upright(Component name) {
        if (name.getStyle().isItalic()) return name;
        MutableComponent copy = name.copy();
        copy.setStyle(copy.getStyle().withItalic(false));
        return copy;
    }
}
