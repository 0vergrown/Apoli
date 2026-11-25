package dev.overgrown.apoli.client.gui;

import dev.overgrown.apoli.gui.GuiElement;
import dev.overgrown.apoli.gui.GuiMenu;
import dev.overgrown.apoli.gui.GuiPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class GuiButtonWidget extends AbstractWidget {
    private final GuiScreen screen;
    private final GuiMenu menu;
    private final int ordinal;
    private final int element;
    private final GuiElement.Button button;
    private boolean pressed;

    public GuiButtonWidget(GuiScreen screen, GuiMenu menu, int ordinal) {
        super(0, 0, 1, 1, menu.button(ordinal).text().orElse(Component.empty()));
        this.screen = screen;
        this.menu = menu;
        this.ordinal = ordinal;
        this.element = menu.layout().buttonElement(ordinal);
        this.button = menu.button(ordinal);
    }

    public void place() {
        screen.place(this, button.placement());
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
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    public void draw(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiPlacement placement = button.placement();
        boolean hovered = isMouseOver(mouseX, mouseY);
        int x = placement.offsetX();
        int y = placement.offsetY();
        int width = placement.width();
        int height = placement.height();
        screen.begin(graphics, placement);
        Optional<ResourceLocation> texture = pick(hovered);
        if (texture.isPresent()) {
            GuiDraw.texture(graphics, texture.get(), x, y, width, height);
        } else {
            GuiDraw.button(graphics, x, y, width, height, active, hovered || pressed);
        }
        if (button.text().isPresent()) {
            graphics.drawCenteredString(Minecraft.getInstance().font, button.text().get(), x + width / 2,
                y + (height - 8) / 2, active ? 0xFFFFFF : 0xA0A0A0);
        }
        screen.end(graphics);
    }

    private Optional<ResourceLocation> pick(boolean hovered) {
        if (button.texture().isEmpty()) return Optional.empty();
        if (!active) return button.disabledTexture().or(button::texture);
        if (pressed) return button.pressedTexture().or(button::hoverTexture).or(button::texture);
        if (hovered) return button.hoverTexture().or(button::texture);
        return button.texture();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonIndex) {
        if (buttonIndex != 0 || !isMouseOver(mouseX, mouseY)) return false;
        pressed = true;
        playDownSound(Minecraft.getInstance().getSoundManager());
        screen.sendButton(ordinal, false);
        return true;
    }

    public boolean releaseIfPressed() {
        if (!pressed) return false;
        pressed = false;
        screen.sendButton(ordinal, true);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
