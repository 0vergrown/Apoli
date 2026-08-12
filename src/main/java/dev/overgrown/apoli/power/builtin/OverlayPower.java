package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public final class OverlayPower extends PowerType<OverlayPower.Config> {
    public record Config(
        ResourceLocation texture,
        float strength,
        float red,
        float green,
        float blue,
        DrawMode drawMode,
        DrawPhase drawPhase,
        boolean hideWithHud,
        boolean visibleInThirdPerson
    ) {}

    public enum DrawMode implements StringRepresentable {
        TEXTURE("texture"),
        NAUSEA("nausea");

        public static final Codec<DrawMode> CODEC = StringRepresentable.fromEnum(DrawMode::values);
        private final String name;
        DrawMode(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public enum DrawPhase implements StringRepresentable {
        BELOW_HUD("below_hud"),
        ABOVE_HUD("above_hud");

        public static final Codec<DrawPhase> CODEC = StringRepresentable.fromEnum(DrawPhase::values);
        private final String name;
        DrawPhase(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("texture").forGetter(Config::texture),
            Codec.FLOAT.optionalFieldOf("strength", 1f).forGetter(Config::strength),
            Codec.FLOAT.optionalFieldOf("red", 1f).forGetter(Config::red),
            Codec.FLOAT.optionalFieldOf("green", 1f).forGetter(Config::green),
            Codec.FLOAT.optionalFieldOf("blue", 1f).forGetter(Config::blue),
            DrawMode.CODEC.optionalFieldOf("draw_mode", DrawMode.TEXTURE).forGetter(Config::drawMode),
            DrawPhase.CODEC.optionalFieldOf("draw_phase", DrawPhase.ABOVE_HUD).forGetter(Config::drawPhase),
            Codec.BOOL.optionalFieldOf("hide_with_hud", true).forGetter(Config::hideWithHud),
            Codec.BOOL.optionalFieldOf("visible_in_third_person", false).forGetter(Config::visibleInThirdPerson)
        ).apply(i, Config::new));
    }
}
