package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.TextureRef;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;

public final class OverlayPower extends PowerType<OverlayPower.Config> {

    public record Config(List<Entry> overlays) {}

    public record Entry(
        TextureRef texture,
        float strength,
        float red,
        float green,
        float blue,
        DrawMode drawMode,
        DrawPhase drawPhase,
        boolean hideWithHud,
        boolean visibleInThirdPerson,
        Expression x,
        Expression y,
        Optional<Expression> width,
        Optional<Expression> height,
        Expression u,
        Expression v,
        Optional<Integer> textureWidth,
        Optional<Integer> textureHeight,
        Optional<Integer> regionWidth,
        Optional<Integer> regionHeight,
        Anchor anchor,
        Optional<EntityCondition> condition
    ) {
        public boolean shouldRender(EntityCtx ctx) {
            return condition.isEmpty() || condition.get().test(ctx);
        }

        Entry withPlacement(Placement placement) {
            return new Entry(texture, strength, red, green, blue, drawMode, drawPhase, hideWithHud,
                visibleInThirdPerson, placement.x(), placement.y(), placement.width(), placement.height(),
                placement.u(), placement.v(), placement.textureWidth(), placement.textureHeight(),
                placement.regionWidth(), placement.regionHeight(), placement.anchor(), placement.condition());
        }
    }

    public enum DrawMode implements StringRepresentable {
        TEXTURE("texture"),
        NAUSEA("nausea");

        public static final Codec<DrawMode> CODEC = StringRepresentable.fromEnum(DrawMode::values);
        private final String name;
        DrawMode(String n) {
            this.name = n;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum DrawPhase implements StringRepresentable {
        BELOW_HUD("below_hud"),
        ABOVE_HUD("above_hud");

        public static final Codec<DrawPhase> CODEC = StringRepresentable.fromEnum(DrawPhase::values);
        private final String name;
        DrawPhase(String n) {
            this.name = n;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Anchor implements StringRepresentable {
        TOP_LEFT("top_left", 0.0F, 0.0F),
        TOP_CENTER("top_center", 0.5F, 0.0F),
        TOP_RIGHT("top_right", 1.0F, 0.0F),
        LEFT("left", 0.0F, 0.5F),
        CENTER("center", 0.5F, 0.5F),
        RIGHT("right", 1.0F, 0.5F),
        BOTTOM_LEFT("bottom_left", 0.0F, 1.0F),
        BOTTOM_CENTER("bottom_center", 0.5F, 1.0F),
        BOTTOM_RIGHT("bottom_right", 1.0F, 1.0F);

        public static final Codec<Anchor> CODEC = StringRepresentable.fromEnum(Anchor::values);
        private final String name;
        private final float horizontal;
        private final float vertical;

        Anchor(String name, float horizontal, float vertical) {
            this.name = name;
            this.horizontal = horizontal;
            this.vertical = vertical;
        }

        public int originX(int screenWidth, int quadWidth) {
            return Math.round((screenWidth - quadWidth) * this.horizontal);
        }

        public int originY(int screenHeight, int quadHeight) {
            return Math.round((screenHeight - quadHeight) * this.vertical);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private record Placement(
        Expression x,
        Expression y,
        Optional<Expression> width,
        Optional<Expression> height,
        Expression u,
        Expression v,
        Optional<Integer> textureWidth,
        Optional<Integer> textureHeight,
        Optional<Integer> regionWidth,
        Optional<Integer> regionHeight,
        Anchor anchor,
        Optional<EntityCondition> condition
    ) {}

    private static final MapCodec<Entry> APPEARANCE = RecordCodecBuilder.mapCodec(i -> i.group(
        TextureRef.MAP_CODEC.forGetter(Entry::texture),
        Codec.FLOAT.optionalFieldOf("strength", 1f).forGetter(Entry::strength),
        Codec.FLOAT.optionalFieldOf("red", 1f).forGetter(Entry::red),
        Codec.FLOAT.optionalFieldOf("green", 1f).forGetter(Entry::green),
        Codec.FLOAT.optionalFieldOf("blue", 1f).forGetter(Entry::blue),
        DrawMode.CODEC.optionalFieldOf("draw_mode", DrawMode.TEXTURE).forGetter(Entry::drawMode),
        DrawPhase.CODEC.optionalFieldOf("draw_phase", DrawPhase.ABOVE_HUD).forGetter(Entry::drawPhase),
        Codec.BOOL.optionalFieldOf("hide_with_hud", true).forGetter(Entry::hideWithHud),
        Codec.BOOL.optionalFieldOf("visible_in_third_person", false).forGetter(Entry::visibleInThirdPerson)
    ).apply(i, (texture, strength, red, green, blue, drawMode, drawPhase, hideWithHud, thirdPerson) ->
        new Entry(texture, strength, red, green, blue, drawMode, drawPhase, hideWithHud, thirdPerson,
            Expression.constant(0), Expression.constant(0), Optional.empty(), Optional.empty(),
            Expression.constant(0), Expression.constant(0), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Anchor.TOP_LEFT, Optional.empty())));

    private static MapCodec<Placement> placement(boolean withCondition) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Expression.INT_OR_EXPR.optionalFieldOf("x", Expression.constant(0)).forGetter(Placement::x),
            Expression.INT_OR_EXPR.optionalFieldOf("y", Expression.constant(0)).forGetter(Placement::y),
            Expression.INT_OR_EXPR.optionalFieldOf("width").forGetter(Placement::width),
            Expression.INT_OR_EXPR.optionalFieldOf("height").forGetter(Placement::height),
            Expression.INT_OR_EXPR.optionalFieldOf("u", Expression.constant(0)).forGetter(Placement::u),
            Expression.INT_OR_EXPR.optionalFieldOf("v", Expression.constant(0)).forGetter(Placement::v),
            Codec.INT.optionalFieldOf("texture_width").forGetter(Placement::textureWidth),
            Codec.INT.optionalFieldOf("texture_height").forGetter(Placement::textureHeight),
            Codec.INT.optionalFieldOf("region_width").forGetter(Placement::regionWidth),
            Codec.INT.optionalFieldOf("region_height").forGetter(Placement::regionHeight),
            Anchor.CODEC.optionalFieldOf("anchor", Anchor.TOP_LEFT).forGetter(Placement::anchor),
            (withCondition
                ? dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC)
                : MapCodec.unit(Optional.<EntityCondition>empty())).forGetter(Placement::condition)
        ).apply(i, Placement::new));
    }

    private static MapCodec<Entry> entryCodec(boolean withCondition) {
        MapCodec<Placement> placement = placement(withCondition);
        return Codec.mapPair(APPEARANCE, placement).xmap(
            pair -> pair.getFirst().withPlacement(pair.getSecond()),
            entry -> Pair.of(entry, new Placement(entry.x(), entry.y(), entry.width(), entry.height(),
                entry.u(), entry.v(), entry.textureWidth(), entry.textureHeight(), entry.regionWidth(),
                entry.regionHeight(), entry.anchor(), entry.condition())));
    }

    private static final MapCodec<Entry> ENTRY_MAP_CODEC = entryCodec(false);

    public static final Codec<Entry> ENTRY_CODEC = entryCodec(true).codec();

    private static final MapCodec<List<Entry>> LIST_FORM =
        Codec.list(ENTRY_CODEC).fieldOf("overlays");

    private static final MapCodec<Config> CODEC = Codec.mapEither(LIST_FORM, ENTRY_MAP_CODEC).xmap(
        either -> new Config(either.map(list -> list, List::of)),
        config -> Either.left(config.overlays()));

    @Override
    public MapCodec<Config> configCodec() {
        return CODEC;
    }
}
