package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum ScrollDirection {
    UP("up"),
    DOWN("down"),
    ANY("any");

    public static final Codec<ScrollDirection> CODEC = Codec.STRING.comapFlatMap(
        ScrollDirection::byName, ScrollDirection::getSerializedName);

    private final String name;

    ScrollDirection(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return name;
    }

    public boolean accepts(ScrollDirection notch) {
        return this == ANY || this == notch;
    }

    public static ScrollDirection ofDelta(double delta) {
        return delta >= 0 ? UP : DOWN;
    }

    private static DataResult<ScrollDirection> byName(String raw) {
        return switch (raw) {
            case "up", "UP" -> DataResult.success(UP);
            case "down", "DOWN" -> DataResult.success(DOWN);
            case "any", "ANY", "both", "BOTH" -> DataResult.success(ANY);
            default -> DataResult.error(() -> "Unknown scroll direction '" + raw
                + "' — expected one of: up, down, any");
        };
    }
}
