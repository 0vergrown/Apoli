package dev.overgrown.apoli.gui;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.EquipmentSlot;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public sealed interface GuiElement
    permits GuiElement.Image, GuiElement.Button, GuiElement.Slider, GuiElement.EntityView, GuiElement.SlotView {

    GuiPlacement placement();

    Kind kind();

    Codec<GuiElement> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<GuiElement, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                T rawType = map.get("type");
                if (rawType == null) {
                    return DataResult.error(() -> "A GUI element needs a \"type\": apoli:image, apoli:button, "
                        + "apoli:slider, apoli:entity or apoli:slot");
                }
                return ops.getStringValue(rawType).flatMap(Kind::parse)
                    .flatMap(kind -> kind.codec().decode(ops, map).map(element -> Pair.of((GuiElement) element, input)));
            });
        }

        @Override
        public <T> DataResult<T> encode(GuiElement input, DynamicOps<T> ops, T prefix) {
            return input.kind().encode(input, ops, prefix);
        }
    };

    enum Kind {
        IMAGE("image"),
        BUTTON("button"),
        SLIDER("slider"),
        ENTITY("entity"),
        SLOT("slot");

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        public String id() {
            return "apoli:" + name;
        }

        public MapCodec<? extends GuiElement> codec() {
            return switch (this) {
                case IMAGE -> Image.MAP_CODEC;
                case BUTTON -> Button.MAP_CODEC;
                case SLIDER -> Slider.MAP_CODEC;
                case ENTITY -> EntityView.MAP_CODEC;
                case SLOT -> SlotView.MAP_CODEC;
            };
        }

        @SuppressWarnings("unchecked")
        <T> DataResult<T> encode(GuiElement element, DynamicOps<T> ops, T prefix) {
            RecordBuilder<T> builder = ((MapCodec<GuiElement>) codec()).encode(element, ops, ops.mapBuilder());
            builder.add("type", ops.createString(id()));
            return builder.build(prefix);
        }

        static DataResult<Kind> parse(String raw) {
            String path = raw;
            int colon = raw.indexOf(':');
            if (colon >= 0) {
                String namespace = raw.substring(0, colon);
                if (!namespace.equals("apoli") && !namespace.equals("origins") && !namespace.equals("minecraft")) {
                    return DataResult.error(() -> "Unknown GUI element type: " + raw);
                }
                path = raw.substring(colon + 1);
            }
            for (Kind kind : values()) {
                if (kind.name.equals(path)) return DataResult.success(kind);
            }
            return DataResult.error(() -> "Unknown GUI element type: " + raw
                + ". Use apoli:image, apoli:button, apoli:slider, apoli:entity or apoli:slot");
        }
    }

    enum ProgressDirection implements StringRepresentable {
        RIGHT("right"),
        LEFT("left"),
        UP("up"),
        DOWN("down");

        public static final Codec<ProgressDirection> CODEC = Codec.STRING.comapFlatMap(ProgressDirection::parse,
            ProgressDirection::getSerializedName);

        private final String name;

        ProgressDirection(String name) {
            this.name = name;
        }

        static DataResult<ProgressDirection> parse(String raw) {
            String key = raw.toLowerCase(Locale.ROOT);
            if (key.equals("horizontal")) return DataResult.success(RIGHT);
            if (key.equals("vertical")) return DataResult.success(UP);
            for (ProgressDirection direction : values()) {
                if (direction.name.equals(key)) return DataResult.success(direction);
            }
            return DataResult.error(() -> "Unknown progress_direction: " + raw
                + ". Use right, left, up or down (horizontal is right, vertical is up)");
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    enum SlotMode implements StringRepresentable {
        INPUT("input"),
        OUTPUT("output");

        public static final Codec<SlotMode> CODEC = StringRepresentable.fromEnum(SlotMode::values);

        private final String name;

        SlotMode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    record Image(GuiPlacement placement, ResourceLocation texture, Optional<Expression> progress,
                 ProgressDirection direction) implements GuiElement {
        public static final MapCodec<Image> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GuiPlacement.mapCodec(16, 16).forGetter(Image::placement),
            IdCodecs.ID.fieldOf("texture_location").forGetter(Image::texture),
            LoggedOptionalField.strict("progress", Expression.DOUBLE_OR_EXPR).forGetter(Image::progress),
            LoggedOptionalField.of("progress_direction", ProgressDirection.CODEC, ProgressDirection.RIGHT)
                .forGetter(Image::direction)
        ).apply(i, Image::new));

        @Override
        public Kind kind() {
            return Kind.IMAGE;
        }
    }

    record Button(GuiPlacement placement, Optional<ResourceLocation> texture, Optional<ResourceLocation> hoverTexture,
                  Optional<ResourceLocation> pressedTexture, Optional<ResourceLocation> disabledTexture,
                  Optional<Component> text, Optional<EntityAction> onPress, Optional<EntityAction> whilePressed,
                  Optional<EntityAction> onRelease, Optional<EntityCondition> pressCondition) implements GuiElement {
        public static final MapCodec<Button> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GuiPlacement.mapCodec(80, 20).forGetter(Button::placement),
            IdCodecs.ID.optionalFieldOf("texture_location").forGetter(Button::texture),
            IdCodecs.ID.optionalFieldOf("hover_texture_location").forGetter(Button::hoverTexture),
            IdCodecs.ID.optionalFieldOf("pressed_texture_location").forGetter(Button::pressedTexture),
            IdCodecs.ID.optionalFieldOf("disabled_texture_location").forGetter(Button::disabledTexture),
            TextComponent.CODEC.optionalFieldOf("text").forGetter(Button::text),
            LoggedOptionalField.strict("action_on_press", EntityAction.CODEC).forGetter(Button::onPress),
            LoggedOptionalField.strict("action_while_pressed", EntityAction.CODEC).forGetter(Button::whilePressed),
            LoggedOptionalField.strict("action_on_release", EntityAction.CODEC).forGetter(Button::onRelease),
            LoggedOptionalField.strict("press_condition", EntityCondition.CODEC).forGetter(Button::pressCondition)
        ).apply(i, Button::new));

        @Override
        public Kind kind() {
            return Kind.BUTTON;
        }
    }

    record Slider(GuiPlacement placement, Optional<ResourceLocation> background, Optional<ResourceLocation> handle,
                  int handleWidth, double min, double max, double step, Optional<Double> startValue,
                  Optional<Component> text, String id, Optional<EntityAction> onSlide, Optional<EntityAction> onRelease,
                  Optional<EntityCondition> slideCondition) implements GuiElement {
        public static final MapCodec<Slider> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GuiPlacement.mapCodec(150, 20).forGetter(Slider::placement),
            IdCodecs.ID.optionalFieldOf("background_texture_location").forGetter(Slider::background),
            IdCodecs.ID.optionalFieldOf("slider_texture_location").forGetter(Slider::handle),
            LoggedOptionalField.of("slider_width", ExtraCodecs.POSITIVE_INT, 8).forGetter(Slider::handleWidth),
            LoggedOptionalField.of("min", Codec.DOUBLE, 0.0).forGetter(Slider::min),
            LoggedOptionalField.of("max", Codec.DOUBLE, 100.0).forGetter(Slider::max),
            LoggedOptionalField.of("step", Codec.doubleRange(0.0, Double.MAX_VALUE), 1.0).forGetter(Slider::step),
            LoggedOptionalField.of("start_value", Codec.DOUBLE).forGetter(Slider::startValue),
            TextComponent.CODEC.optionalFieldOf("text").forGetter(Slider::text),
            Codec.STRING.optionalFieldOf("id", "").forGetter(Slider::id),
            LoggedOptionalField.strict("action_on_slide", EntityAction.CODEC).forGetter(Slider::onSlide),
            LoggedOptionalField.strict("action_on_release", EntityAction.CODEC).forGetter(Slider::onRelease),
            LoggedOptionalField.strict("slide_condition", EntityCondition.CODEC).forGetter(Slider::slideCondition)
        ).apply(i, Slider::new));

        @Override
        public Kind kind() {
            return Kind.SLIDER;
        }

        public double range() {
            return Math.max(0.0, max - min);
        }

        public double snap(double value) {
            if (!Double.isFinite(value)) value = min;
            double clamped = Mth.clamp(value, min, Math.max(min, max));
            if (step <= 0.0) return clamped;
            double steps = Math.round((clamped - min) / step);
            return Math.min(min + steps * step, Math.max(min, max));
        }

        public double start() {
            return snap(startValue.orElse(min));
        }

        public double fraction(double value) {
            double range = range();
            return range <= 0.0 ? 0.0 : Mth.clamp((value - min) / range, 0.0, 1.0);
        }

        public double valueAt(double fraction) {
            return snap(min + Mth.clamp(fraction, 0.0, 1.0) * range());
        }

        public String storageKey(int ordinal) {
            return id.isEmpty() ? "#" + ordinal : id;
        }
    }

    record Equipment(EquipmentSlot slot, ItemStack item) {
        public static final Codec<Equipment> CODEC = RecordCodecBuilder.create(i -> i.group(
            EquipmentSlot.CODEC.fieldOf("slot").forGetter(Equipment::slot),
            ItemStackData.CODEC.xmap(ItemStackData::stack, ItemStackData::new).fieldOf("item").forGetter(Equipment::item)
        ).apply(i, Equipment::new));
    }

    record EntityView(GuiPlacement placement, Optional<ResourceLocation> entityType, List<Equipment> equipment,
                      Optional<String> selector, Optional<ResourceLocation> set, boolean followMouse,
                      Optional<Float> scale) implements GuiElement {
        public static final MapCodec<EntityView> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GuiPlacement.mapCodec(50, 70).forGetter(EntityView::placement),
            IdCodecs.ID.optionalFieldOf("entity_type").forGetter(EntityView::entityType),
            Equipment.CODEC.listOf().optionalFieldOf("equipment", List.of()).forGetter(EntityView::equipment),
            Codec.STRING.optionalFieldOf("selector").forGetter(EntityView::selector),
            IdCodecs.ID.optionalFieldOf("set").forGetter(EntityView::set),
            Codec.BOOL.optionalFieldOf("follow_mouse", true).forGetter(EntityView::followMouse),
            LoggedOptionalField.of("scale", ExtraCodecs.POSITIVE_FLOAT).forGetter(EntityView::scale)
        ).apply(i, EntityView::new));

        @Override
        public Kind kind() {
            return Kind.ENTITY;
        }

        public boolean tracksWorldEntity() {
            return selector.isPresent() || set.isPresent();
        }
    }

    record SlotView(GuiPlacement placement, SlotMode mode, Optional<ItemCondition> itemCondition)
        implements GuiElement {
        public static final MapCodec<SlotView> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            GuiPlacement.mapCodec(18, 18).forGetter(SlotView::placement),
            LoggedOptionalField.of("mode", SlotMode.CODEC, SlotMode.INPUT).forGetter(SlotView::mode),
            LoggedOptionalField.strict("item_condition", ItemCondition.CODEC).forGetter(SlotView::itemCondition)
        ).apply(i, SlotView::new));

        @Override
        public Kind kind() {
            return Kind.SLOT;
        }
    }
}
