package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

public enum BiEntitySide implements StringRepresentable {
    ACTOR("actor"),
    TARGET("target");

    public static final Codec<BiEntitySide> CODEC = StringRepresentable.fromEnum(BiEntitySide::values);

    private final String name;

    BiEntitySide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public BiEntitySide opposite() {
        return this == ACTOR ? TARGET : ACTOR;
    }

    public Entity of(BiEntityCtx ctx) {
        return this == ACTOR ? ctx.actor() : ctx.target();
    }
}
