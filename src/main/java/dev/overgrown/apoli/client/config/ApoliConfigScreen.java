package dev.overgrown.apoli.client.config;

import dev.overgrown.apoli.client.ApoliClientConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

@Environment(EnvType.CLIENT)
public class ApoliConfigScreen extends Screen {

    private static final List<String> SPEECH_SOURCES = List.of("auto", "microphone", "voicechat");

    private static final int ROW_HEIGHT = 24;
    private static final int OFFSET_RANGE = 64;
    private static final int WIDGET_WIDTH = 260;

    @Nullable
    private final Screen parent;
    @Nullable
    private EditBox inputDevice;
    @Nullable
    private Integer hudOffsetX;
    @Nullable
    private Integer hudOffsetY;

    public ApoliConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("apoli.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ApoliClientConfig config = ApoliClientConfig.get();
        int x = this.width / 2 - WIDGET_WIDTH / 2;
        int y = Math.max(20, this.height / 2 - ROW_HEIGHT * 5);

        this.addRenderableWidget(toggle("speech_to_action", config.speechToAction(), x, y, config::setSpeechToAction));
        y += ROW_HEIGHT;
        this.addRenderableWidget(toggle("speech_push_to_talk", config.speechPushToTalk(), x, y, config::setSpeechPushToTalk));
        y += ROW_HEIGHT;
        this.addRenderableWidget(toggle("speech_instant", config.speechInstant(), x, y, config::setSpeechInstant));
        y += ROW_HEIGHT;
        this.addRenderableWidget(toggle("speech_echo", config.speechEcho(), x, y, config::setSpeechEcho));
        y += ROW_HEIGHT;

        this.addRenderableWidget(CycleButton.<String>builder(ApoliConfigScreen::speechSourceLabel)
            .withValues(SPEECH_SOURCES)
            .withInitialValue(SPEECH_SOURCES.contains(config.speechSource()) ? config.speechSource() : "auto")
            .withTooltip(value -> Tooltip.create(Component.translatable("apoli.config.speech_source.tooltip")))
            .create(x, y, WIDGET_WIDTH, 20,
                Component.translatable("apoli.config.speech_source"),
                (button, value) -> config.setSpeechSource(value)));
        y += ROW_HEIGHT;

        this.inputDevice = new EditBox(this.font, x, y, WIDGET_WIDTH, 20,
            Component.translatable("apoli.config.speech_input_device"));
        this.inputDevice.setMaxLength(128);
        this.inputDevice.setValue(config.speechInputDevice());
        this.inputDevice.setHint(Component.translatable("apoli.config.speech_input_device.hint"));
        this.inputDevice.setTooltip(Tooltip.create(Component.translatable("apoli.config.speech_input_device.tooltip")));
        this.addRenderableWidget(this.inputDevice);
        y += ROW_HEIGHT;

        this.addRenderableWidget(toggle("hud_auto_stack", config.hudAutoStack(), x, y, config::setHudAutoStack));
        y += ROW_HEIGHT;

        int half = (WIDGET_WIDTH - 8) / 2;
        this.addRenderableWidget(new OffsetSlider(x, y, half, "apoli.config.hud_offset_x",
            this.hudOffsetX == null ? config.hudOffsetX() : this.hudOffsetX, value -> this.hudOffsetX = value));
        this.addRenderableWidget(new OffsetSlider(x + half + 8, y, half, "apoli.config.hud_offset_y",
            this.hudOffsetY == null ? config.hudOffsetY() : this.hudOffsetY, value -> this.hudOffsetY = value));
        y += ROW_HEIGHT + 8;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(this.width / 2 - 100, y, 200, 20)
            .build());
    }

    private CycleButton<Boolean> toggle(String key, boolean initial, int x, int y, BooleanSetter setter) {
        return CycleButton.onOffBuilder(initial)
            .withTooltip(value -> Tooltip.create(Component.translatable("apoli.config." + key + ".tooltip")))
            .create(x, y, WIDGET_WIDTH, 20, Component.translatable("apoli.config." + key),
                (button, value) -> setter.set(value));
    }

    private static Component speechSourceLabel(String value) {
        return Component.translatable("apoli.config.speech_source." + value.toLowerCase(Locale.ROOT));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        ApoliClientConfig config = ApoliClientConfig.get();
        if (this.inputDevice != null) {
            config.setSpeechInputDevice(this.inputDevice.getValue().trim());
        }
        if (this.hudOffsetX != null) {
            config.setHudOffsetX(this.hudOffsetX);
        }
        if (this.hudOffsetY != null) {
            config.setHudOffsetY(this.hudOffsetY);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    private static final class OffsetSlider extends AbstractSliderButton {

        private final String key;
        private final IntConsumer sink;

        private OffsetSlider(int x, int y, int width, String key, int offset, IntConsumer sink) {
            super(x, y, width, 20, Component.empty(),
                Math.max(0.0, Math.min(1.0, (offset + OFFSET_RANGE) / (double) (OFFSET_RANGE * 2))));
            this.key = key;
            this.sink = sink;
            this.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
            this.updateMessage();
        }

        private int offset() {
            return (int) Math.round(this.value * (OFFSET_RANGE * 2)) - OFFSET_RANGE;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(this.key, this.offset()));
        }

        @Override
        protected void applyValue() {
            this.sink.accept(this.offset());
        }
    }
}
