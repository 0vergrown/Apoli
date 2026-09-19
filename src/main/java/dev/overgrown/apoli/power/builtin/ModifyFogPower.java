package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.PowerType;

import java.util.Optional;

public final class ModifyFogPower extends PowerType<ModifyFogPower.Config> {
    public record Config(
            Optional<Expression> s,
            Optional<Expression> v,

            Optional<Expression> r,
            Optional<Expression> g,
            Optional<Expression> b,

            Expression fadeIn,
            Expression fadeOut,

            int priority
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.FLOAT_OR_EXPR.optionalFieldOf("s").forGetter(Config::s),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("v").forGetter(Config::v),

                Expression.FLOAT_OR_EXPR.optionalFieldOf("r").forGetter(Config::r),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("g").forGetter(Config::g),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("b").forGetter(Config::b),

                Expression.FLOAT_OR_EXPR.optionalFieldOf("fade_in", Expression.constant(0f)).forGetter(Config::fadeIn),
                Expression.FLOAT_OR_EXPR.optionalFieldOf("fade_out", Expression.constant(0f)).forGetter(Config::fadeOut),

                Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(i, Config::new));
    }
}
