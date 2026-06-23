package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ClipContext;

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

    public ClipContext.Block vanilla() {
        return switch (this) {
            case COLLIDER -> ClipContext.Block.COLLIDER;
            case OUTLINE -> ClipContext.Block.OUTLINE;
            case VISUAL -> ClipContext.Block.VISUAL;
        };
    }
}
