package dev.overgrown.apoli.script;

import dev.overgrown.apoli.Apoli;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ApoliScripts {
    private ApoliScripts() {}

    public enum Kind {
        ENTITY_ACTION,
        BIENTITY_ACTION,
        BLOCK_ACTION,
        ITEM_ACTION,
        ENTITY_CONDITION,
        BIENTITY_CONDITION,
        BLOCK_CONDITION,
        ITEM_CONDITION,
        POWER_ADDED,
        POWER_REMOVED
    }

    private static final java.util.EnumSet<Kind> ACTION_KINDS = java.util.EnumSet.of(
        Kind.ENTITY_ACTION, Kind.BIENTITY_ACTION, Kind.BLOCK_ACTION, Kind.ITEM_ACTION,
        Kind.POWER_ADDED, Kind.POWER_REMOVED);

    private record Key(Kind kind, ResourceLocation id) {}

    private static final Map<Key, Consumer<ScriptCtx>> ACTIONS = new ConcurrentHashMap<>();
    private static final Map<Key, Predicate<ScriptCtx>> CONDITIONS = new ConcurrentHashMap<>();

    @FunctionalInterface
    public interface EventDispatcher {
        boolean dispatch(ResourceLocation id, ScriptCtx ctx);
    }

    @FunctionalInterface
    public interface EventPredicate {
        @Nullable
        Boolean test(ResourceLocation id, ScriptCtx ctx);
    }

    private static final Map<Kind, EventDispatcher> EVENT_ACTIONS = new ConcurrentHashMap<>();
    private static final Map<Kind, EventPredicate> EVENT_CONDITIONS = new ConcurrentHashMap<>();

    @Nullable
    private static volatile ScriptBackend backend;

    public static void setEventDispatcher(Kind kind, EventDispatcher dispatcher) {
        EVENT_ACTIONS.put(kind, dispatcher);
    }

    public static void setEventPredicate(Kind kind, EventPredicate predicate) {
        EVENT_CONDITIONS.put(kind, predicate);
    }

    public static void setBackend(ScriptBackend next) {
        backend = next;
        Apoli.LOGGER.info("[Apoli] Script backend installed: {}", next.name());
    }

    @Nullable
    public static ScriptBackend backend() {
        return backend;
    }

    public static void registerAction(Kind kind, ResourceLocation id, Consumer<ScriptCtx> handler) {
        if (!ACTION_KINDS.contains(kind)) {
            throw new IllegalArgumentException(kind + " is not an action kind");
        }
        ACTIONS.put(new Key(kind, id), handler);
    }

    public static void registerCondition(Kind kind, ResourceLocation id, Predicate<ScriptCtx> handler) {
        CONDITIONS.put(new Key(kind, id), handler);
    }

    @Nullable
    public static Consumer<ScriptCtx> action(Kind kind, ResourceLocation id) {
        return ACTIONS.get(new Key(kind, id));
    }

    @Nullable
    public static Predicate<ScriptCtx> condition(Kind kind, ResourceLocation id) {
        return CONDITIONS.get(new Key(kind, id));
    }

    public static void run(Kind kind, ResourceLocation id, ScriptCtx ctx) {
        Consumer<ScriptCtx> handler = ACTIONS.get(new Key(kind, id));
        if (handler != null) {
            try {
                handler.accept(ctx);
            } catch (Throwable t) {
                ScriptWarnings.failed(kind, id, t);
            }
            return;
        }
        EventDispatcher dispatcher = EVENT_ACTIONS.get(kind);
        if (dispatcher != null) {
            try {
                if (dispatcher.dispatch(id, ctx)) return;
            } catch (Throwable t) {
                ScriptWarnings.failed(kind, id, t);
                return;
            }
        }
        ScriptBackend engine = backend;
        if (engine != null && engine.available() && engine.has(id)) {
            try {
                engine.execute(id, ctx);
            } catch (Throwable t) {
                ScriptWarnings.failed(kind, id, t);
            }
            return;
        }
        ScriptWarnings.missing(kind, id);
    }

    public static boolean test(Kind kind, ResourceLocation id, ScriptCtx ctx, boolean fallback) {
        Predicate<ScriptCtx> handler = CONDITIONS.get(new Key(kind, id));
        if (handler != null) {
            try {
                return handler.test(ctx);
            } catch (Throwable t) {
                ScriptWarnings.failed(kind, id, t);
                return fallback;
            }
        }
        EventPredicate predicate = EVENT_CONDITIONS.get(kind);
        if (predicate != null) {
            try {
                Boolean result = predicate.test(id, ctx);
                if (result != null) return result;
            } catch (Throwable t) {
                ScriptWarnings.failed(kind, id, t);
                return fallback;
            }
        }
        ScriptBackend engine = backend;
        if (engine != null && engine.available() && engine.has(id)) {
            try {
                return truthy(engine.execute(id, ctx), fallback);
            } catch (Throwable t) {
                ScriptWarnings.failed(kind, id, t);
                return fallback;
            }
        }
        ScriptWarnings.missing(kind, id);
        return fallback;
    }

    private static boolean truthy(@Nullable Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.doubleValue() != 0.0;
        if (value instanceof CharSequence text) return !text.isEmpty();
        return true;
    }

    public static void clear() {
        ACTIONS.clear();
        CONDITIONS.clear();
        ScriptWarnings.reset();
    }

    public static int registeredCount() {
        return ACTIONS.size() + CONDITIONS.size();
    }
}
