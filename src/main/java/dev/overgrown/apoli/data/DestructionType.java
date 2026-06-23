package dev.overgrown.apoli.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Explosion;

public enum DestructionType implements StringRepresentable {
    BREAK("break", Explosion.BlockInteraction.DESTROY),
    NONE("none", Explosion.BlockInteraction.KEEP),
    DESTROY("destroy", Explosion.BlockInteraction.DESTROY_WITH_DECAY);

    public static final Codec<DestructionType> CODEC = StringRepresentable.fromEnum(DestructionType::values);

    private final String name;
    private final Explosion.BlockInteraction vanilla;

    DestructionType(String name, Explosion.BlockInteraction vanilla) {
        this.name = name;
        this.vanilla = vanilla;
    }

    public Explosion.BlockInteraction vanilla() {
        return vanilla;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
