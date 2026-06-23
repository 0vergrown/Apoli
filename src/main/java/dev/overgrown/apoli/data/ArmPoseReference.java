package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ArmPoseReference implements StringRepresentable {

    EMPTY("empty"),
    ITEM("item"),
    BLOCK("block"),
    BOW_AND_ARROW("bow_and_arrow"),
    THROW_SPEAR("throw_spear"),
    CROSSBOW_CHARGE("crossbow_charge"),
    CROSSBOW_HOLD("crossbow_hold"),
    SPYGLASS("spyglass"),
    TOOT_HORN("toot_horn"),
    BRUSH("brush");

    public static final Codec<ArmPoseReference> CODEC = StringRepresentable.fromEnum(ArmPoseReference::values);

    private final String name;

    ArmPoseReference(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
