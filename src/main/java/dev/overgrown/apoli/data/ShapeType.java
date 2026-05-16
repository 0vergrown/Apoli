package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ShapeType implements StringRepresentable {
    COLLIDER("collider"),
    OUTLINE("outline"),
    VISUAL("visual");

    public static final Codec<ShapeType> CODEC = StringRepresentable.fromEnum(ShapeType::values);

    private final String name;

    ShapeType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}