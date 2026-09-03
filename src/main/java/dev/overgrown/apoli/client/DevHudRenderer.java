package dev.overgrown.apoli.client;

import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class DevHudRenderer {

    private static final int HEADER_COLOUR = 0xFF55FFFF;
    private static final int LINE_COLOUR = 0xFFFFFFFF;

    private DevHudRenderer() {}

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!ClientDevMode.enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        PowerContainer container = PowerContainer.of(player);
        if (container == null) return;

        List<String> lines = new ArrayList<>();
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            if (!PowerResources.isResource(powerId)) continue;
            int size = Math.max(1, PowerResources.size(container, powerId));
            for (int slot = 0; slot < size; slot++) {
                OptionalInt value = size > 1
                    ? PowerResources.readAt(container, powerId, slot)
                    : PowerResources.read(container, powerId);
                if (value.isEmpty()) continue;
                OptionalInt max = PowerResources.bound(container, powerId, true);
                StringBuilder line = new StringBuilder();
                line.append(value.getAsInt());
                if (max.isPresent()) line.append('/').append(max.getAsInt());
                line.append(" - ").append(powerId);
                if (size > 1) line.append('[').append(slot).append(']');
                lines.add(line.toString());
            }
        }
        lines.sort(String::compareTo);

        int x = 4;
        int y = 4;
        graphics.drawString(mc.font, Component.literal("Apoli dev mode — cooldowns off"), x, y, HEADER_COLOUR, true);
        y += 12;
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(mc.font, Component.literal(lines.get(i)), x, y, LINE_COLOUR, true);
            y += 10;
        }
    }
}
