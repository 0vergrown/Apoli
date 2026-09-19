package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

import java.util.Optional;

public final class PrintAction implements ActionType<EntityCtx, PrintAction.Cfg> {
    public record Cfg(Optional<String> text, String message_id, Optional<Expression> number, boolean show_in_chat) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("text").forGetter(Cfg::text),
                Codec.STRING.fieldOf("message_id").forGetter(Cfg::message_id),
                Expression.INT_OR_EXPR.optionalFieldOf("number").forGetter(PrintAction.Cfg::number),
                Codec.BOOL.optionalFieldOf("show_in_chat", false).forGetter(PrintAction.Cfg::show_in_chat)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Logger LOGGER = LoggerFactory.getLogger((cfg.message_id));
        ServerPlayer player = (ServerPlayer) ctx.entity();

        if ((cfg.text).isPresent() && ((cfg.number).isPresent())) {
            String text = cfg.text().get();
            String number = String.valueOf(cfg.number().get().constantValue().getAsDouble());

            LOGGER.info("Text: {} Number: {}", text, number);
            if (cfg.show_in_chat) { ctx.raw().sendSystemMessage(Component.literal("Text: " + text + " Number: " + number)); }
        } else if ((cfg.text).isPresent()) {
            LOGGER.info(cfg.text().get());
            if (cfg.show_in_chat) { ctx.raw().sendSystemMessage(Component.literal(cfg.text().get())); }
        } else {
            String number = String.valueOf(cfg.number().get().constantValue().getAsDouble());

            LOGGER.info(number);
            if (cfg.show_in_chat) { ctx.raw().sendSystemMessage(Component.literal(number)); }
        }
    }
}
