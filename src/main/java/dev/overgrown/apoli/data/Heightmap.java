package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public enum Heightmap implements StringRepresentable {
    WORLD_SURFACE("world_surface", Types.WORLD_SURFACE),
    WORLD_SURFACE_WG("world_surface_wg", Types.WORLD_SURFACE_WG),
    OCEAN_FLOOR("ocean_floor", Types.OCEAN_FLOOR),
    OCEAN_FLOOR_WG("ocean_floor_wg", Types.OCEAN_FLOOR_WG),
    MOTION_BLOCKING("motion_blocking", Types.MOTION_BLOCKING),
    MOTION_BLOCKING_NO_LEAVES("motion_blocking_no_leaves", Types.MOTION_BLOCKING_NO_LEAVES);

    public static final Codec<Heightmap> CODEC = StringRepresentable.fromEnum(Heightmap::values);

    private final String name;
    private final Types vanilla;

    Heightmap(String name, Types vanilla) {
        this.name = name;
        this.vanilla = vanilla;
    }

    public Types vanilla() {
        return vanilla;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
