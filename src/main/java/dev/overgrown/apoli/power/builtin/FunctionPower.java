package dev.overgrown.apoli.power.builtin;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class FunctionPower extends PowerType<FunctionPower.Cfg> {

    private static final Logger LOG = LogUtils.getLogger();
    private static final int MAX_CACHE_ENTRIES = 8;
    private static final int MAX_DEPTH = 16;

    private static final DecimalFormat NUMBER_FORMAT = Util.make(new DecimalFormat("#"), format -> {
        format.setMaximumFractionDigits(15);
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
    });

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
        Set<String> found = new LinkedHashSet<>();
        collectPlaceholders(raw.entityAction(), found);
        List<String> parameters = raw.parameters().orElseGet(() -> List.copyOf(found));

        for (String declared : parameters) {
            if (!found.contains(declared)) {
                return DataResult.error(() -> "apoli:function declares parameter [" + declared
                    + "] but never uses it");
            }
        }
        for (String used : found) {
            if (!parameters.contains(used)) {
                return DataResult.error(() -> "apoli:function uses [" + used
                    + "] but does not declare it in \"parameters\"");
            }
        }

        if (!parameters.isEmpty()) {
            return DataResult.success(new Cfg(raw.entityAction(), parameters, null));
        }
        return parseAction(raw.entityAction())
            .map(action -> new Cfg(raw.entityAction(), List.of(), action));
    }

    private static <T> DataResult<EntityAction> parseAction(Dynamic<T> source) {
        return EntityAction.CODEC.parse(source.getOps(), source.getValue());
    }

    private static <T> void collectPlaceholders(Dynamic<T> source, Set<String> out) {
        collectPlaceholders(source.getOps(), source.getValue(), out);
    }

    private static <T> void collectPlaceholders(DynamicOps<T> ops, T input, Set<String> out) {
        Optional<String> text = ops.getStringValue(input).result();
        if (text.isPresent()) {
            collectNames(text.get(), out);
            return;
        }
        Optional<Stream<Pair<T, T>>> entries = ops.getMapValues(input).result();
        if (entries.isPresent()) {
            entries.get().forEach(entry -> collectPlaceholders(ops, entry.getSecond(), out));
            return;
        }
        ops.getStream(input).result()
            .ifPresent(values -> values.forEach(value -> collectPlaceholders(ops, value, out)));
    }

    private static void collectNames(String text, Set<String> out) {
        int from = 0;
        while (true) {
            int open = text.indexOf('[', from);
            if (open < 0) return;
            int close = text.indexOf(']', open + 1);
            if (close < 0) return;
            String name = text.substring(open + 1, close);
            if (!name.isEmpty() && name.indexOf('[') < 0) out.add(name);
            from = close + 1;
        }
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
        return EntityAction.CODEC.parse(ops, substitute(ops, source.getValue(), parameters, arguments));
    }

    private static <T> T substitute(DynamicOps<T> ops, T input, List<String> parameters,
                                    Map<String, Dynamic<?>> arguments) {
        Optional<String> text = ops.getStringValue(input).result();
        if (text.isPresent()) {
            return substituteString(ops, input, text.get(), parameters, arguments);
        }
        Optional<Stream<Pair<T, T>>> entries = ops.getMapValues(input).result();
        if (entries.isPresent()) {
            return ops.createMap(entries.get().map(entry -> Pair.of(entry.getFirst(),
                substitute(ops, entry.getSecond(), parameters, arguments))));
        }
        Optional<Stream<T>> values = ops.getStream(input).result();
        if (values.isPresent()) {
            return ops.createList(values.get().map(value -> substitute(ops, value, parameters, arguments)));
        }
        return input;
    }

    private static <T> T substituteString(DynamicOps<T> ops, T input, String text, List<String> parameters,
                                          Map<String, Dynamic<?>> arguments) {
        for (int i = 0; i < parameters.size(); i++) {
            String parameter = parameters.get(i);
            if (text.equals("[" + parameter + "]")) {
                return arguments.get(parameter).convert(ops).getValue();
            }
        }
        String replaced = text;
        for (int i = 0; i < parameters.size(); i++) {
            String parameter = parameters.get(i);
            String placeholder = "[" + parameter + "]";
            if (replaced.contains(placeholder)) {
                replaced = replaced.replace(placeholder, stringify(arguments.get(parameter)));
            }
        }
        return replaced.equals(text) ? input : ops.createString(replaced);
    }

    private static <T> String stringify(Dynamic<T> value) {
        DynamicOps<T> ops = value.getOps();
        T raw = value.getValue();
        Optional<String> text = ops.getStringValue(raw).result();
        if (text.isPresent()) return text.get();
        Optional<Number> number = ops.getNumberValue(raw).result();
        if (number.isPresent()) return NUMBER_FORMAT.format(number.get());
        Optional<Boolean> flag = ops.getBooleanValue(raw).result();
        if (flag.isPresent()) return flag.get().toString();
        return String.valueOf(raw);
    }
}
