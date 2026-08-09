package dev.overgrown.apoli.client;

import dev.overgrown.apoli.data.TextBar;
import dev.overgrown.apoli.network.payload.TextDisplayS2C;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@OnlyIn(Dist.CLIENT)
public final class TextOverlayRenderer {
    private TextOverlayRenderer() {}

    private static final class Entry {
        Component text;
        int fadeIn;
        int fadeOut;
        long shownAt;
        long steadyUntil;
    }

    private static final Entry[] SLOTS = new Entry[TextBar.VALUES.length];
    private static final float ABOVE_CHAT_Z = 200.0f;
    private static long guiTicks;

    public static void apply(TextDisplayS2C payload) {
        int slot = payload.bar().ordinal();
        if (payload.text().getString().isEmpty() && payload.text().getSiblings().isEmpty()) {
            SLOTS[slot] = null;
            return;
        }
        Entry entry = SLOTS[slot];
        boolean sameText = entry != null && payload.text().equals(entry.text);
        if (!sameText) {
            entry = new Entry();
            entry.text = payload.text();
            entry.shownAt = guiTicks;
            SLOTS[slot] = entry;
        }
        entry.fadeIn = Math.max(0, payload.fadeIn());
        entry.fadeOut = Math.max(0, payload.fadeOut());
        entry.steadyUntil = payload.stay() < 0 ? Long.MAX_VALUE : guiTicks + entry.fadeIn + payload.stay();
    }

    public static void clear() {
        java.util.Arrays.fill(SLOTS, null);
    }

    public static void tick() {
        guiTicks++;
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        boolean any = false;
        for (Entry slot : SLOTS) {
            if (slot != null) {
                any = true;
                break;
            }
        }
        if (!any) return;

        Font font = mc.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        for (int i = 0; i < SLOTS.length; i++) {
            Entry entry = SLOTS[i];
            if (entry == null) continue;
            float age = (guiTicks - entry.shownAt) + partialTick;
            float alpha = alphaFor(entry, age, partialTick);
            if (alpha <= 0.0f) {
                if (entry.steadyUntil != Long.MAX_VALUE && guiTicks > entry.steadyUntil + entry.fadeOut) {
                    SLOTS[i] = null;
                }
                continue;
            }
            int alphaBits = (int) (alpha * 255.0f) << 24;
            if ((alphaBits & 0xFC000000) == 0) continue;
            int color = 0xFFFFFF | alphaBits;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0f, 0.0f, ABOVE_CHAT_Z);
            draw(graphics, font, TextBar.VALUES[i], entry.text, color, width, height);
            graphics.pose().popPose();
        }
    }

    private static float alphaFor(Entry entry, float age, float partialTick) {
        if (entry.fadeIn > 0 && age < entry.fadeIn) {
            return age / entry.fadeIn;
        }
        if (entry.steadyUntil == Long.MAX_VALUE) return 1.0f;
        float sinceSteadyEnd = (guiTicks - entry.steadyUntil) + partialTick;
        if (sinceSteadyEnd <= 0) return 1.0f;
        if (entry.fadeOut <= 0) return 0.0f;
        return Mth.clamp(1.0f - (sinceSteadyEnd / entry.fadeOut), 0.0f, 1.0f);
    }

    private static void draw(GuiGraphics graphics, Font font, TextBar bar, Component text, int color, int width, int height) {
        int textWidth = font.width(text);
        switch (bar) {
            case TITLE -> {
                graphics.pose().pushPose();
                graphics.pose().translate(width / 2.0f, height / 2.0f, 0.0f);
                graphics.pose().scale(4.0f, 4.0f, 4.0f);
                graphics.drawString(font, text, -textWidth / 2, -10, color, true);
                graphics.pose().popPose();
            }
            case SUBTITLE -> {
                graphics.pose().pushPose();
                graphics.pose().translate(width / 2.0f, height / 2.0f, 0.0f);
                graphics.pose().scale(2.0f, 2.0f, 2.0f);
                graphics.drawString(font, text, -textWidth / 2, 5, color, true);
                graphics.pose().popPose();
            }
            case ACTIONBAR -> graphics.drawString(font, text, (width - textWidth) / 2, height - 68, color, true);
            case TOP_LEFT -> graphics.drawString(font, text, 4, 4, color, true);
            case TOP_CENTER -> graphics.drawString(font, text, (width - textWidth) / 2, 24, color, true);
            case TOP_RIGHT -> graphics.drawString(font, text, width - textWidth - 4, 4, color, true);
            case LEFT -> graphics.drawString(font, text, 4, (height - 9) / 2, color, true);
            case RIGHT -> graphics.drawString(font, text, width - textWidth - 4, (height - 9) / 2, color, true);
            case BOTTOM_LEFT -> graphics.drawString(font, text, 4, height - 50, color, true);
            case BOTTOM_RIGHT -> graphics.drawString(font, text, width - textWidth - 4, height - 50, color, true);
        }
    }
}
