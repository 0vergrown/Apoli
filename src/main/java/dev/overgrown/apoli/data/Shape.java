package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;

public enum Shape implements StringRepresentable {
    CUBE("cube"),
    SPHERE("sphere"),
    STAR("star"),
    CONE("cone");

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
        return contains(dx, dy, dz, radius, radius, radius);
    }

    
    public boolean contains(int dx, int dy, int dz, int rx, int ry, int rz) {
        return switch (this) {
            case CUBE -> Math.abs(dx) <= rx && Math.abs(dy) <= ry && Math.abs(dz) <= rz;
            case SPHERE -> {
                double nx = norm(dx, rx), ny = norm(dy, ry), nz = norm(dz, rz);
                yield nx * nx + ny * ny + nz * nz <= 1.0;
            }
            case STAR -> {
                double nx = norm(dx, rx), ny = norm(dy, ry), nz = norm(dz, rz);
                yield nx + ny + nz <= 1.0;
            }
            case CONE -> {
                
                if (dy < 0 || dy > ry) {
                    yield false;
                }
                double t = ry <= 0 ? 1.0 : (double) dy / ry;
                double nx = norm(dx, rx), nz = norm(dz, rz);
                yield Math.sqrt(nx * nx + nz * nz) <= t;
            }
        };
    }

    private static double norm(int delta, int radius) {
        if (radius <= 0) return delta == 0 ? 0.0 : Double.POSITIVE_INFINITY;
        return (double) Math.abs(delta) / radius;
    }

    public List<BlockPos> positions(BlockPos center, int radius) {
        return positions(center, radius, radius, radius);
    }

    public List<BlockPos> positions(BlockPos center, int rx, int ry, int rz) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    if (contains(dx, dy, dz, rx, ry, rz)) {
                        out.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }
        return out;
    }
}
