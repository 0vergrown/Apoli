package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FluidVisionPower extends PowerType<FluidVisionPower.Config> {

    public enum Submersion implements StringRepresentable {
        NONE("none", FogType.NONE),
        WATER("water", FogType.WATER),
        LAVA("lava", FogType.LAVA),
        POWDER_SNOW("powder_snow", FogType.POWDER_SNOW);

        public static final Codec<Submersion> CODEC = StringRepresentable.fromEnum(Submersion::values);

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

    public record FogColor(float red, float green, float blue) {
        public static final Codec<FogColor> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("red", 0f).forGetter(FogColor::red),
            Codec.FLOAT.optionalFieldOf("green", 0f).forGetter(FogColor::green),
            Codec.FLOAT.optionalFieldOf("blue", 0f).forGetter(FogColor::blue)
        ).apply(i, FogColor::new));
    }

    public record Config(Submersion fluid, float start, float end, Optional<FogColor> fogColor, boolean renderOverlay) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Submersion.CODEC.optionalFieldOf("fluid", Submersion.LAVA).forGetter(Config::fluid),
            Codec.FLOAT.optionalFieldOf("start", 0f).forGetter(Config::start),
            Codec.FLOAT.optionalFieldOf("end", 15f).forGetter(Config::end),
            FogColor.CODEC.optionalFieldOf("fog_color").forGetter(Config::fogColor),
            Codec.BOOL.optionalFieldOf("render_overlay", true).forGetter(Config::renderOverlay)
        ).apply(i, Config::new));
    }

    @Nullable
    public static Config activeFor(@Nullable Entity entity, FogType fog) {
        if (entity == null || fog == null) return null;
        Config[] best = {null};
        PowerLookup.forEach(entity, ApoliIds.FLUID_VISION, Config.class, cfg -> {
            if (cfg.fluid.fog() != fog) return;
            if (best[0] == null || cfg.end > best[0].end) best[0] = cfg;
        });
        return best[0];
    }
}
