package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum HitSide implements StringRepresentable {
    SELF("self"),
    ATTACKER("attacker"),
    TARGET("target");

    public static final Codec<HitSide> CODEC = StringRepresentable.fromEnum(HitSide::values);

    private final String name;

    HitSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
