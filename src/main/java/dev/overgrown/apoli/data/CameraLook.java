package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum CameraLook implements StringRepresentable {
    ANCHOR("anchor"),
    HOLDER("holder"),
    FIXED("fixed"),
    AT_HOLDER("at_holder"),
    AT_ANCHOR("at_anchor");

    public static final Codec<CameraLook> CODEC = StringRepresentable.fromEnum(CameraLook::values);

    private final String name;

    CameraLook(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean looksAt() {
        return this == AT_HOLDER || this == AT_ANCHOR;
    }
}
