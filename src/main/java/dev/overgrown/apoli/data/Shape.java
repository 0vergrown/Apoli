package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;

public enum Shape implements StringRepresentable {
    CUBE("cube"),
    SPHERE("sphere"),
    STAR("star");

    public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

    private final String name;

    Shape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean contains(int dx, int dy, int dz, int radius) {
        return switch (this) {
            case CUBE -> Math.abs(dx) <= radius && Math.abs(dy) <= radius && Math.abs(dz) <= radius;
            case SPHERE -> (dx * dx + dy * dy + dz * dz) <= radius * radius;
            case STAR -> (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) <= radius;
        };
    }

    public List<BlockPos> positions(BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (contains(dx, dy, dz, radius)) {
                        out.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }
        return out;
    }
}
