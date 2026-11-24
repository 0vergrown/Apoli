package dev.overgrown.apoli.station;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum StationCheck implements StringRepresentable {
    LIT("lit"),
    WORKING("working"),
    HAS_INPUT("has_input"),
    HAS_FUEL("has_fuel"),
    HAS_OUTPUT("has_output");

    public static final Codec<StationCheck> CODEC = StringRepresentable.fromEnum(StationCheck::values);

    private final String name;

    StationCheck(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
