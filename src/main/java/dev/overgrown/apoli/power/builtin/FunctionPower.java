package dev.overgrown.apoli.power.builtin;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Placeholders;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FunctionPower extends PowerType<FunctionPower.Cfg> {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int MAX_CACHE_ENTRIES = 8;
    private static final int MAX_DEPTH = 16;

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    public static final class Cfg {
        private final Dynamic<?> source;
        private final List<String> parameters;
        private final @Nullable EntityAction constant;
        private final Map<List<Dynamic<?>>, EntityAction> cache =
            new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true);

        Cfg(Dynamic<?> source, List<String> parameters, @Nullable EntityAction constant) {
            this.source = source;
            this.parameters = List.copyOf(parameters);
            this.constant = constant;
        }

        public Dynamic<?> source() {
            return source;
        }

        public List<String> parameters() {
            return parameters;
        }

        public @Nullable EntityAction constant() {
            return constant;
        }
    }

    private record Raw(Dynamic<?> entityAction, Optional<List<String>> parameters) {}

    public static final MapCodec<Cfg> CODEC = RecordCodecBuilder.<Raw>mapCodec(i -> i.group(
        Codec.PASSTHROUGH.fieldOf("entity_action").forGetter(Raw::entityAction),
        Codec.STRING.listOf().optionalFieldOf("parameters").forGetter(Raw::parameters)
    ).apply(i, Raw::new)).flatXmap(FunctionPower::build, cfg ->
        DataResult.success(new Raw(cfg.source(), Optional.of(cfg.parameters()))));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CODEC;
    }

    private static DataResult<Cfg> build(Raw raw) {
        return Placeholders.parameters("apoli:function", raw.entityAction(), raw.parameters()).flatMap(parameters -> {
            if (!parameters.isEmpty()) {
                return DataResult.success(new Cfg(raw.entityAction(), parameters, null));
            }
            return parseAction(raw.entityAction())
                .map(action -> new Cfg(raw.entityAction(), List.of(), action));
        });
    }

    private static <T> DataResult<EntityAction> parseAction(Dynamic<T> source) {
        return EntityAction.CODEC.parse(source.getOps(), source.getValue());
    }

    public static void run(ResourceLocation powerId, Cfg cfg, Map<String, Dynamic<?>> arguments, EntityCtx ctx) {
        int[] depth = DEPTH.get();
        if (depth[0] >= MAX_DEPTH) {
            if (FunctionWarnings.first(powerId, "depth")) {
                LOG.warn("[Apoli] apoli:run_function stopped at depth {} on {} — check for a function that calls itself.",
                    MAX_DEPTH, powerId);
            }
            return;
        }
        EntityAction action = instantiate(powerId, cfg, arguments);
        if (action == null) return;
        depth[0]++;
        try {
            action.run(ctx);
        } finally {
            depth[0]--;
        }
    }

    private static @Nullable EntityAction instantiate(ResourceLocation powerId, Cfg cfg,
                                                      Map<String, Dynamic<?>> arguments) {
        if (cfg.constant() != null) return cfg.constant();

        List<Dynamic<?>> key = new ArrayList<>(cfg.parameters().size());
        for (String parameter : cfg.parameters()) {
            Dynamic<?> value = arguments.get(parameter);
            if (value == null) {
                if (FunctionWarnings.first(powerId, "missing:" + parameter)) {
                    LOG.warn("[Apoli] apoli:run_function on {} is missing argument [{}].", powerId, parameter);
                }
                return null;
            }
            key.add(value);
        }

        synchronized (cfg.cache) {
            EntityAction cached = cfg.cache.get(key);
            if (cached != null) return cached;
        }

        EntityAction parsed = instantiate(cfg.source(), cfg.parameters(), arguments)
            .resultOrPartial(err -> LOG.error("[Apoli] apoli:function {} failed to build with {}: {}",
                powerId, arguments, err))
            .orElse(null);
        if (parsed == null) return null;

        synchronized (cfg.cache) {
            cfg.cache.put(key, parsed);
            if (cfg.cache.size() > MAX_CACHE_ENTRIES) {
                var oldest = cfg.cache.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
        }
        return parsed;
    }

    private static <T> DataResult<EntityAction> instantiate(Dynamic<T> source, List<String> parameters,
                                                            Map<String, Dynamic<?>> arguments) {
        DynamicOps<T> ops = source.getOps();
        return EntityAction.CODEC.parse(ops, Placeholders.substitute(ops, source.getValue(), parameters, arguments));
    }
}
