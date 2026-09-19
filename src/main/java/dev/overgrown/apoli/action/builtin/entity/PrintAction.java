package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class PrintAction implements ActionType<EntityCtx, PrintAction.Cfg> {
    public record Cfg(Optional<String> text, String message_id, Optional<Expression> number) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("text").forGetter(Cfg::text),
                Codec.STRING.fieldOf("message_id").forGetter(Cfg::message_id),
                Expression.INT_OR_EXPR.optionalFieldOf("number").forGetter(PrintAction.Cfg::number)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Logger LOGGER = LoggerFactory.getLogger((cfg.message_id));

        if ((cfg.text).isPresent()) {
            LOGGER.info(cfg.text().get());
        } else {
            LOGGER.info(cfg.number().get().toString());
        }
    }
}
