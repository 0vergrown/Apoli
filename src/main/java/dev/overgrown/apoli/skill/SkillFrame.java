package dev.overgrown.apoli.skill;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SkillFrame implements StringRepresentable {
    TASK("task"),
    GOAL("goal"),
    CHALLENGE("challenge");

    public static final Codec<SkillFrame> CODEC = StringRepresentable.fromEnum(SkillFrame::values);
    public static final SkillFrame[] VALUES = values();

    private final String name;

    SkillFrame(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
