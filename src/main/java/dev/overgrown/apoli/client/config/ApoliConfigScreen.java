package dev.overgrown.apoli.client.config;

import dev.overgrown.apoli.client.ApoliClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
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

@OnlyIn(Dist.CLIENT)
public class ApoliConfigScreen extends Screen {

    private static final List<String> SPEECH_SOURCES = List.of("auto", "microphone", "voicechat");

    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 260;

    @Nullable
    private final Screen parent;
    @Nullable
    private EditBox inputDevice;

    public ApoliConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("apoli.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ApoliClientConfig config = ApoliClientConfig.get();
        int x = this.width / 2 - WIDGET_WIDTH / 2;
        int y = Math.max(40, this.height / 2 - ROW_HEIGHT * 4);

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
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.inputDevice != null) {
            ApoliClientConfig.get().setSpeechInputDevice(this.inputDevice.getValue().trim());
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }
}
