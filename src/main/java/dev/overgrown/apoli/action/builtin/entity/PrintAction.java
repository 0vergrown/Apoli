package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class PrintAction implements ActionType<EntityCtx, PrintAction.Cfg> {

    private static final String DEFAULT_MESSAGE_ID = "apoli/print";

    public record Cfg(Optional<Component> text, String messageId, Optional<Expression> number,
                      boolean showInChat, Logger logger) {

        public Cfg(Optional<Component> text, String messageId, Optional<Expression> number, boolean showInChat) {
            this(text, messageId, number, showInChat, LoggerFactory.getLogger(messageId));
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LoggedOptionalField.of("text", TextComponent.CODEC).forGetter(Cfg::text),
            Codec.STRING.optionalFieldOf("message_id", DEFAULT_MESSAGE_ID).forGetter(Cfg::messageId),
            LoggedOptionalField.of("number", Expression.DOUBLE_OR_EXPR).forGetter(Cfg::number),
            Codec.BOOL.optionalFieldOf("show_in_chat", false).forGetter(Cfg::showInChat)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.raw();
        ServerPlayer viewer = cfg.showInChat && entity instanceof ServerPlayer player ? player : null;
        boolean toLog = cfg.logger.isInfoEnabled();
        if (viewer == null && !toLog) return;

        Component text = cfg.text.orElse(null);
        String number = cfg.number.isPresent() ? format(cfg.number.get().eval(entity)) : null;

        if (toLog) {
            if (text != null && number != null) cfg.logger.info("Text: {} Number: {}", text.getString(), number);
            else if (text != null) cfg.logger.info("{}", text.getString());
            else if (number != null) cfg.logger.info("{}", number);
            else cfg.logger.info("{}", cfg.messageId);
        }
        if (viewer != null) viewer.sendSystemMessage(chatMessage(cfg, text, number));
    }

    private static Component chatMessage(Cfg cfg, @Nullable Component text, @Nullable String number) {
        if (text != null && number != null) {
            return Component.literal("Text: ").append(text).append(" Number: " + number);
        }
        if (text != null) return text;
        if (number != null) return Component.literal(number);
        return Component.literal(cfg.messageId);
    }

    private static String format(double value) {
        return value == (long) value ? Long.toString((long) value) : Double.toString(value);
    }
}
