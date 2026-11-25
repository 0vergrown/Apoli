package dev.overgrown.apoli.client.gui;

import dev.overgrown.apoli.gui.GuiElement;
import dev.overgrown.apoli.gui.GuiMenu;
import dev.overgrown.apoli.gui.GuiPlacement;
import dev.overgrown.apoli.power.PowerResources;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class GuiSliderWidget extends AbstractWidget {
    private static final String TOKEN = "$(slider)";

    private final GuiScreen screen;
    private final GuiMenu menu;
    private final int ordinal;
    private final int element;
    private final GuiElement.Slider slider;
    private boolean dragging;
    private double value;
    @Nullable
    private Component label;
    private double labelValue = Double.NaN;

    public GuiSliderWidget(GuiScreen screen, GuiMenu menu, int ordinal) {
        super(0, 0, 1, 1, Component.empty());
        this.screen = screen;
        this.menu = menu;
        this.ordinal = ordinal;
        this.element = menu.layout().sliderElement(ordinal);
        this.slider = menu.slider(ordinal);
        this.value = menu.sliderValue(ordinal);
    }

    public void place() {
        screen.place(this, slider.placement());
    }

    public void bounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        this.width = width;
        this.height = height;
    }

    public void sync() {
        this.visible = menu.isVisible(element);
        this.active = menu.isEnabled(element);
        if (!dragging) value = menu.sliderValue(ordinal);
        if (dragging && (!visible || !active)) releaseIfDragging();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    public void draw(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiPlacement placement = slider.placement();
        boolean hovered = dragging || isMouseOver(mouseX, mouseY);
        int x = placement.offsetX();
        int y = placement.offsetY();
        int width = placement.width();
        int height = placement.height();
        int handleWidth = Math.min(slider.handleWidth(), width);
        int handleX = x + (int) Math.round(slider.fraction(value) * (width - handleWidth));
        screen.begin(graphics, placement);
        if (slider.background().isPresent()) {
            GuiDraw.texture(graphics, slider.background().get(), x, y, width, height);
        } else {
            GuiDraw.slider(graphics, x, y, width, height, hovered);
        }
        if (slider.handle().isPresent()) {
            GuiDraw.texture(graphics, slider.handle().get(), handleX, y, handleWidth, height);
        } else {
            GuiDraw.handle(graphics, handleX, y, handleWidth, height, hovered && active);
        }
        Component text = label();
        if (text != null) {
            graphics.drawCenteredString(Minecraft.getInstance().font, text, x + width / 2, y + (height - 8) / 2,
                active ? 0xFFFFFF : 0xA0A0A0);
        }
        screen.end(graphics);
    }

    @Nullable
    private Component label() {
        if (slider.text().isEmpty()) return null;
        if (label == null || labelValue != value) {
            label = substitute(slider.text().get(), PowerResources.format(value));
            labelValue = value;
            setMessage(label);
        }
        return label;
    }

    private static Component substitute(Component text, String replacement) {
        MutableComponent out = Component.empty();
        text.visit((style, chunk) -> {
            out.append(Component.literal(chunk.replace(TOKEN, replacement)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        dragging = true;
        follow(mouseX);
        return true;
    }

    public boolean drag(double mouseX) {
        if (!dragging) return false;
        follow(mouseX);
        return true;
    }

    public boolean releaseIfDragging() {
        if (!dragging) return false;
        dragging = false;
        playDownSound(Minecraft.getInstance().getSoundManager());
        screen.sendSlider(ordinal, value, true);
        return true;
    }

    private void follow(double mouseX) {
        GuiPlacement placement = slider.placement();
        int handleWidth = Math.min(slider.handleWidth(), placement.width());
        double local = screen.localX(placement, mouseX) - placement.offsetX();
        int track = placement.width() - handleWidth;
        double fraction = track <= 0 ? 0.0 : (local - handleWidth / 2.0) / track;
        double next = slider.valueAt(fraction);
        if (next == value) return;
        value = next;
        menu.setClientSlider(ordinal, value);
        screen.sendSlider(ordinal, value, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
