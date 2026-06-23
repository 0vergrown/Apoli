package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.FogType;

import java.util.Optional;

public final class ModifyCameraSubmersionPower extends PowerType<ModifyCameraSubmersionPower.Config> {
    public record Config(Optional<Submersion> from, Submersion to) {}

    public enum Submersion implements StringRepresentable {
        NONE("none", FogType.NONE),
        WATER("water", FogType.WATER),
        LAVA("lava", FogType.LAVA);

        public static final Codec<Submersion> CODEC = StringRepresentable.fromEnum(Submersion::values);

        private final String name;
        private final FogType vanilla;
        Submersion(String name, FogType vanilla) { this.name = name; this.vanilla = vanilla; }

        public FogType vanilla() { return vanilla; }

        @Override public String getSerializedName() { return name; }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Submersion.CODEC.optionalFieldOf("from").forGetter(Config::from),
            Submersion.CODEC.fieldOf("to").forGetter(Config::to)
        ).apply(i, Config::new));
    }
}
