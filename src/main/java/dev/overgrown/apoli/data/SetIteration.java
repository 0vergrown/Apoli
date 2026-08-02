package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SetIteration implements StringRepresentable {
    MEMBERS("members"),
    OWNERS("owners");

    public static final Codec<SetIteration> CODEC = StringRepresentable.fromEnum(SetIteration::values);

    private final String name;

    SetIteration(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
