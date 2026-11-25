package dev.overgrown.apoli.gui;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record GuiPlacement(int x, int y, GuiAnchor anchor, int width, int height, int guiScaleLock,
                           Optional<EntityCondition> condition) {

    public static MapCodec<GuiPlacement> mapCodec(int defaultWidth, int defaultHeight) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("x", 0).forGetter(GuiPlacement::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(GuiPlacement::y),
            LoggedOptionalField.of("anchor", GuiAnchor.CODEC, GuiAnchor.TOP_LEFT).forGetter(GuiPlacement::anchor),
            LoggedOptionalField.of("width", ExtraCodecs.POSITIVE_INT, defaultWidth).forGetter(GuiPlacement::width),
            LoggedOptionalField.of("height", ExtraCodecs.POSITIVE_INT, defaultHeight).forGetter(GuiPlacement::height),
            LoggedOptionalField.of("gui_scale_lock", ExtraCodecs.NON_NEGATIVE_INT, 0).forGetter(GuiPlacement::guiScaleLock),
            LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(GuiPlacement::condition)
        ).apply(i, GuiPlacement::new));
    }

    public int anchorX(int boxWidth) {
        return anchor.x(boxWidth);
    }

    public int anchorY(int boxHeight) {
        return anchor.y(boxHeight);
    }

    public int left(int boxWidth) {
        return anchor.x(boxWidth) - anchor.x(width) + x;
    }

    public int top(int boxHeight) {
        return anchor.y(boxHeight) - anchor.y(height) + y;
    }

    public int offsetX() {
        return x - anchor.x(width);
    }

    public int offsetY() {
        return y - anchor.y(height);
    }
}
