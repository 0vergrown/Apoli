package dev.overgrown.apoli.tick;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TickScope implements StringRepresentable {
    ENTITY("entity"),
    CHUNK("chunk"),
    DIMENSION("dimension"),
    SERVER("server");

    public static final Codec<TickScope> CODEC = StringRepresentable.fromEnum(TickScope::values);

    private final String name;

    TickScope(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
