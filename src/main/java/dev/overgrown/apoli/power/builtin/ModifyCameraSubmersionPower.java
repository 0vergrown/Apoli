package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeGate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ModifyCameraSubmersionPower extends PowerType<ModifyCameraSubmersionPower.Config> {
    private static final PowerTypeGate IN_USE = new PowerTypeGate(ApoliIds.MODIFY_CAMERA_SUBMERSION);


    public enum Submersion implements StringRepresentable {
        NONE("none", FogType.NONE),
        WATER("water", FogType.WATER),
        LAVA("lava", FogType.LAVA),
        POWDER_SNOW("powder_snow", FogType.POWDER_SNOW);

        public static final com.mojang.serialization.Codec<Submersion> CODEC =
            StringRepresentable.fromEnum(Submersion::values);

        private final String name;
        private final FogType fog;

        Submersion(String name, FogType fog) {
            this.name = name;
            this.fog = fog;
        }

        public FogType fog() {
            return fog;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Config(Submersion from, Submersion to) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Submersion.CODEC.fieldOf("from").forGetter(Config::from),
            Submersion.CODEC.fieldOf("to").forGetter(Config::to)
        ).apply(i, Config::new));
    }

    public static FogType remap(@Nullable Entity entity, FogType original) {
        if (entity == null || !IN_USE.inUse() || !PowerLookup.holdsType(entity, ApoliIds.MODIFY_CAMERA_SUBMERSION)) {
            return original;
        }
        FogType[] result = {original};
        PowerLookup.forEach(entity, ApoliIds.MODIFY_CAMERA_SUBMERSION, Config.class, cfg -> {
            if (cfg.from.fog() == result[0]) result[0] = cfg.to.fog();
        });
        return result[0];
    }
}
