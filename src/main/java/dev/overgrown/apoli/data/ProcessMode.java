package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ProcessMode implements StringRepresentable {
    ITEMS("items"),
    STACKS("stacks");

    public static final Codec<ProcessMode> CODEC = StringRepresentable.fromEnum(ProcessMode::values);

    private final String name;
    ProcessMode(String n) { this.name = n; }

    @Override public String getSerializedName() { return name; }
}
