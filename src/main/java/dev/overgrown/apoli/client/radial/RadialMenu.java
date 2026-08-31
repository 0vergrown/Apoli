package dev.overgrown.apoli.client.radial;

import dev.overgrown.apoli.network.payload.RadialMenuOpenS2C;
import dev.overgrown.apoli.network.payload.RadialMenuSelectC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RadialMenu {

    private final List<RadialMenuOpenS2C.Entry> entries;
    private final ResourceLocation menuTexture;
    private final int nonce;

    private final Button[] buttons;
    private final float[] posX;
    private final float[] posY;
    private boolean buttonsInitialized = false;

    public RadialMenu(List<RadialMenuOpenS2C.Entry> entries, ResourceLocation menuTexture, int nonce) {
        this.entries = entries;
        this.menuTexture = menuTexture;
        this.nonce = nonce;
        this.buttons = new Button[entries.size()];
        this.posX = new float[entries.size()];
        this.posY = new float[entries.size()];
    }

    public Button[] getButtons() {
        return buttons;
    }

    public void draw(Minecraft client, int elapsedTime) {
        positionEntries(client, elapsedTime);

        if (!buttonsInitialized) {
            for (int i = 0; i < entries.size(); i++) {
                RadialMenuOpenS2C.Entry entry = entries.get(i);
                int buttonWidth = entry.buttonWidth();
                int buttonHeight = entry.buttonHeight();

                Component tooltipText = entry.tooltip().orElse(null);
                if (tooltipText == null && !entry.item().isEmpty()) {
                    tooltipText = Component.literal(entry.item().getHoverName().getString());
                }

                final int index = i;
                Button button = Button.builder(Component.empty(), widget -> select(index))
                    .pos(-100, 0)
                    .size(buttonWidth, buttonHeight)
                    .tooltip(tooltipText != null ? Tooltip.create(tooltipText) : null)
                    .build();
                button.active = true;
                button.visible = true;
                buttons[i] = button;
            }
            buttonsInitialized = true;
        }

        for (int i = 0; i < entries.size(); i++) {
            Button button = buttons[i];
            if (button != null) {
                button.setX(Math.round(posX[i]));
                button.setY(Math.round(posY[i] - 1));
            }
        }
    }

    public void renderBackground(GuiGraphics context, Minecraft client) {
        if (menuTexture != null) {
            int centerX = client.getWindow().getGuiScaledWidth() / 2;
            int centerY = client.getWindow().getGuiScaledHeight() / 2;
            int textureSize = 256;
            int halfSize = textureSize / 2;
            context.blit(menuTexture, centerX - halfSize, centerY - halfSize,
                0.0F, 0.0F, textureSize, textureSize, textureSize, textureSize);
        }
    }

    public void renderButtons(GuiGraphics context, int mouseX, int mouseY, float delta) {
        for (int i = 0; i < entries.size(); i++) {
            Button button = buttons[i];
            if (button == null) continue;
            RadialMenuOpenS2C.Entry entry = entries.get(i);

            ResourceLocation buttonTexture = entry.buttonTexture().orElse(null);
            ResourceLocation highlightButtonTexture = entry.highlightButtonTexture().orElse(null);

            boolean isHovered = button.isHoveredOrFocused();
            if (isHovered && highlightButtonTexture != null) {
                buttonTexture = highlightButtonTexture;
            }

            if (buttonTexture != null) {
                button.setAlpha(0.0F);
                button.render(context, mouseX, mouseY, delta);
                button.setAlpha(1.0F);
                context.blit(buttonTexture, button.getX(), button.getY(), 0.0F, 0.0F,
                    button.getWidth(), button.getHeight(), button.getWidth(), button.getHeight());
            } else {
                button.render(context, mouseX, mouseY, delta);
            }
        }
    }

    public void renderIcons(GuiGraphics context) {
        for (int i = 0; i < entries.size(); i++) {
            Button button = buttons[i];
            if (button == null) continue;
            RadialMenuOpenS2C.Entry entry = entries.get(i);

            ResourceLocation icon = entry.icon().orElse(null);
            ResourceLocation highlightIcon = entry.highlightIcon().orElse(null);

            boolean isHovered = button.isHoveredOrFocused();
            if (isHovered && highlightIcon != null) {
                icon = highlightIcon;
            }

            int buttonX = button.getX();
            int buttonY = button.getY();
            int buttonWidth = button.getWidth();
            int buttonHeight = button.getHeight();

            if (icon != null) {
                int iconWidth = entry.iconWidth();
                int iconHeight = entry.iconHeight();
                int iconX = buttonX + (buttonWidth - iconWidth) / 2;
                int iconY = buttonY + (buttonHeight - iconHeight) / 2;
                context.blit(icon, iconX, iconY, 0.0F, 0.0F, iconWidth, iconHeight, iconWidth, iconHeight);
            } else if (!entry.item().isEmpty()) {
                ItemStack stack = entry.item();
                int itemWidth = entry.itemWidth();
                int itemHeight = entry.itemHeight();
                int itemX = buttonX + (buttonWidth - itemWidth) / 2;
                int itemY = buttonY + (buttonHeight - itemHeight) / 2;

                context.pose().pushPose();
                context.pose().translate(itemX, itemY, 0.0F);
                context.pose().scale(itemWidth / 16.0F, itemHeight / 16.0F, 1.0F);
                context.renderItem(stack, 0, 0, 0, 100);
                context.pose().popPose();
            }
        }
    }

    private void positionEntries(Minecraft client, int elapsedTime) {
        int count = entries.size();
        if (count == 0) return;
        float angleInterval = 360.0F / count;
        for (int i = 0; i < count; i++) {
            float centerX = client.getWindow().getGuiScaledWidth() / 2.0F;
            float centerY = client.getWindow().getGuiScaledHeight() / 2.0F;

            RadialMenuOpenS2C.Entry entry = entries.get(i);
            float angle = entry.angle().orElse(angleInterval * i);
            int maxDistance = entry.distance() != -1 ? entry.distance() : client.getWindow().getGuiScaledHeight() / 4;
            float velocity = entry.velocity() != -1 ? entry.velocity() : maxDistance / 3.0F;
            float distance = velocity * elapsedTime < maxDistance ? velocity * elapsedTime : maxDistance;
            float progress = maxDistance == 0 ? 1.0F : distance / maxDistance;

            float px = (float) (centerX + distance * Math.cos(Math.toRadians(angle))) + entry.offsetX() * progress;
            float py = (float) (centerY + distance * Math.sin(Math.toRadians(angle))) + entry.offsetY() * progress;

            posX[i] = px - entry.buttonWidth() / 2.0F;
            posY[i] = py - entry.buttonHeight() / 2.0F;
        }
    }

    public void resetButtons() {
        buttonsInitialized = false;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = null;
        }
    }

    private void select(int index) {
        ClientPlayNetworking.send(new RadialMenuSelectC2S(nonce, index));
        Minecraft.getInstance().setScreen(null);
    }
}
