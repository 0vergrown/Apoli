package dev.overgrown.apoli.station;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum FuelMode implements StringRepresentable {
    ITEMS("items"),
    NONE("none"),
    HUNGER("hunger"),
    RESOURCE("resource");

    public static final Codec<FuelMode> CODEC = StringRepresentable.fromEnum(FuelMode::values);

    private final String name;

    FuelMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
