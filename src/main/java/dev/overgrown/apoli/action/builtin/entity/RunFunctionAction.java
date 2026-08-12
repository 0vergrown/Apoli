package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.FunctionPower;
import dev.overgrown.apoli.power.builtin.FunctionWarnings;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Map;

public final class RunFunctionAction implements ActionType<EntityCtx, RunFunctionAction.Cfg> {

    private static final Logger LOG = LogUtils.getLogger();

    public record Cfg(ResourceLocation function, Map<String, Dynamic<?>> arguments) {
        public Cfg {
            arguments = Map.copyOf(arguments);
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            IdCodecs.ID.fieldOf("function").forGetter(Cfg::function),
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
                .optionalFieldOf("arguments", Map.of()).forGetter(Cfg::arguments)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Power power = ApoliPowers.get(cfg.function());
        if (power == null) {
            if (FunctionWarnings.first(cfg.function(), "unknown")) {
                LOG.warn("[Apoli] apoli:run_function referenced unknown power {}.", cfg.function());
            }
            return;
        }
        if (!(power.config() instanceof FunctionPower.Cfg function)) {
            if (FunctionWarnings.first(cfg.function(), "not_function")) {
                LOG.warn("[Apoli] apoli:run_function referenced {}, which is not an apoli:function.", cfg.function());
            }
            return;
        }
        if (power.condition().isPresent() && !power.condition().get().test(ctx)) return;
        FunctionPower.run(cfg.function(), function, cfg.arguments(), ctx);
    }
}
