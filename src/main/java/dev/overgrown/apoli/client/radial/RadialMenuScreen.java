package dev.overgrown.apoli.client.radial;

import dev.overgrown.apoli.network.payload.RadialMenuOpenS2C;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RadialMenuScreen extends Screen {

    private int elapsedTime;
    private final RadialMenu radialMenu;

    public RadialMenuScreen(RadialMenuOpenS2C payload) {
        super(Component.translatable("screen.apoli.radial_menu"));
        this.radialMenu = new RadialMenu(payload.entries(), payload.sprite().orElse(null), payload.nonce());
        this.elapsedTime = 0;
    }

    @Override
    public void tick() {
        elapsedTime += 1;
        radialMenu.draw(this.minecraft, elapsedTime);
    }

    @Override
    protected void init() {
        radialMenu.resetButtons();
        radialMenu.draw(this.minecraft, elapsedTime);
        for (Button button : radialMenu.getButtons()) {
            if (button != null) {
                addRenderableWidget(button);
            }
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (this.minecraft != null) {
            radialMenu.renderBackground(context, this.minecraft);
            radialMenu.renderButtons(context, mouseX, mouseY, delta);
            radialMenu.renderIcons(context);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        radialMenu.resetButtons();
    }
}
