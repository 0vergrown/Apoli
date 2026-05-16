package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.FluidState;

public enum FluidHandling implements StringRepresentable {
    ANY("any") {
        @Override public boolean test(FluidState state) {
            return !state.isEmpty();
        }
    },
    NONE("none") {
        @Override public boolean test(FluidState state) {
            return state.isEmpty();
        }
    },
    SOURCE_ONLY("source_only") {
        @Override public boolean test(FluidState state) {
            return state.isSource();
        }
    };

    public static final Codec<FluidHandling> CODEC = StringRepresentable.fromEnum(FluidHandling::values);

    private final String name;

    FluidHandling(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public abstract boolean test(FluidState state);
}