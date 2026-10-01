package dev.overgrown.apoli.macros;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.data.Placeholders;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class MacroExpander<T> {

    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation MACRO = Apoli.id("macro");
    private static final ResourceLocation MULTIPLE = Apoli.id("multiple");
    private static final Set<String> CALL_FIELDS = Set.of("type", "macro", "arguments");
    private static final Set<String> DEFINITION_FIELDS = Set.of("type", "parameters", "value");
    private static final int MAX_DEPTH = 32;
    private static final int MAX_VALUES = 250_000;

    private final DynamicOps<T> ops;
    private final T argumentsKey;
    private final Map<ResourceLocation, Macro> macros = new HashMap<>();
    private final Map<ResourceLocation, String> broken = new HashMap<>();
    private final Set<ResourceLocation> definitionFiles = new HashSet<>();
    private final Map<ResourceLocation, List<String>> nestedDefinitions = new HashMap<>();
    private final ArrayDeque<ResourceLocation> stack = new ArrayDeque<>();
    private final List<Object> path = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Set<String> reported = new HashSet<>();
    private @Nullable ResourceLocation file;
    private int values;
    private int calls;
    private int powers;

    private MacroExpander(DynamicOps<T> ops) {
        this.ops = ops;
        this.argumentsKey = ops.createString("arguments");
    }

    public static <T> MacroExpander<T> collect(DynamicOps<T> ops, Map<ResourceLocation, T> files) {
        MacroExpander<T> expander = new MacroExpander<>(ops);
        for (Map.Entry<ResourceLocation, T> entry : files.entrySet()) {
            expander.collectFile(entry.getKey(), entry.getValue());
        }
        expander.file = null;
        return expander;
    }

    public static boolean isMacroType(String type) {
        if (!type.endsWith(":macro")) return false;
        ResourceLocation declared = ResourceLocation.tryParse(type);
        return declared != null && MACRO.equals(NamespaceAlias.resolve(declared));
    }

    public @Nullable Dynamic<T> expand(ResourceLocation id, Dynamic<T> power) {
        if (definitionFiles.contains(id)) return null;
        List<String> nested = nestedDefinitions.get(id);
        T input = power.getValue();
        T stripped = nested == null ? input : strip(input, nested);
        if (macros.isEmpty() && broken.isEmpty()) {
            return stripped == input ? power : new Dynamic<>(ops, stripped);
        }
        file = id;
        values = 0;
        path.clear();
        stack.clear();
        int before = calls;
        try {
            T output = resolve(stripped, id);
            if (calls > before) powers++;
            return output == input ? power : new Dynamic<>(ops, output);
        } catch (Overflow overflow) {
            calls = before;
            path.clear();
            stack.clear();
            error("expanding its macros went past " + MAX_VALUES + " values, so the power was not loaded. "
                + "A macro that calls another macro more than once at every level grows exponentially.");
            return null;
        } finally {
            file = null;
        }
    }

    public MacroReport finish() {
        MacroReport report = new MacroReport(macros.size(), calls, powers, List.copyOf(errors), List.copyOf(warnings));
        if (!report.isEmpty()) {
            LOG.info("[Apoli] Macros: {} defined, {} call(s) expanded in {} power(s), {} error(s), {} warning(s).",
                report.defined(), report.calls(), report.powers(), errors.size(), warnings.size());
        }
        MacroReport.publish(report);
        return report;
    }

    private void collectFile(ResourceLocation id, T value) {
        MapLike<T> fields = ops.getMap(value).result().orElse(null);
        if (fields == null) return;
        String type = stringAt(fields, "type");
        if (type == null) return;
        if (isMacroType(type)) {
            if (fields.get("macro") != null) return;
            file = id;
            definitionFiles.add(id);
            define(id, id, fields, value);
            return;
        }
        if (!isMultiple(type)) return;
        List<String> keys = null;
        List<Pair<T, T>> entries = fields.entries().toList();
        for (int i = 0; i < entries.size(); i++) {
            Pair<T, T> entry = entries.get(i);
            String key = ops.getStringValue(entry.getFirst()).result().orElse(null);
            if (key == null || MultiplePower.RESERVED_FIELDS.contains(key)) continue;
            MapLike<T> sub = ops.getMap(entry.getSecond()).result().orElse(null);
            if (sub == null || sub.get("macro") != null) continue;
            String subType = stringAt(sub, "type");
            if (subType == null || !isMacroType(subType)) continue;
            if (keys == null) keys = new ArrayList<>(2);
            keys.add(key);
            file = id;
            ResourceLocation subId = ResourceLocation.tryParse(id + "_" + key);
            if (subId == null) {
                error("the macro \"" + key + "\" would get an invalid id, so it was not defined");
                continue;
            }
            define(subId, id, sub, entry.getSecond());
        }
        if (keys != null) nestedDefinitions.put(id, List.copyOf(keys));
    }

    private void define(ResourceLocation id, ResourceLocation scope, MapLike<T> fields, T definition) {
        DataResult<Macro> parsed = fields.get("value") == null
            ? DataResult.<Macro>error(() -> "it has no \"value\"")
            : Macro.parse(id, scope, new Dynamic<>(ops, definition));
        Macro macro = parsed.result().orElse(null);
        if (macro == null) {
            String reason = messageOf(parsed);
            broken.put(id, reason);
            error("macro " + id + " could not be loaded: " + reason);
            return;
        }
        if (macros.put(id, macro) != null) {
            warning("macro " + id + " is defined twice, and only one of the definitions is used");
        }
        String ignored = extraFields(fields, DEFINITION_FIELDS);
        if (ignored != null) {
            warning("macro " + id + " ignores " + ignored + ". A macro definition only reads \"parameters\" and \"value\".");
        }
    }

    private T strip(T input, List<String> keys) {
        T out = input;
        for (int i = 0; i < keys.size(); i++) out = ops.remove(out, keys.get(i));
        return out;
    }

    private T resolve(T node, ResourceLocation scope) {
        if (node == null) return null;
        if (++values > MAX_VALUES) throw new Overflow();
        MapLike<T> map = ops.getMap(node).result().orElse(null);
        if (map != null) {
            String type = stringAt(map, "type");
            if (type != null && isMacroType(type)) return call(node, map, scope);
            return resolveEntries(node, map, scope);
        }
        List<T> items = ops.getStream(node).result().map(Stream::toList).orElse(null);
        return items == null ? node : resolveItems(node, items, scope);
    }

    private T resolveEntries(T node, MapLike<T> map, ResourceLocation scope) {
        List<Pair<T, T>> entries = map.entries().toList();
        List<Pair<T, T>> rebuilt = null;
        boolean tracking = stack.isEmpty();
        for (int i = 0; i < entries.size(); i++) {
            Pair<T, T> entry = entries.get(i);
            if (tracking) path.add(entry.getFirst());
            T value = resolve(entry.getSecond(), scope);
            if (tracking) path.remove(path.size() - 1);
            if (value != entry.getSecond() && rebuilt == null) rebuilt = new ArrayList<>(entries.subList(0, i));
            if (rebuilt != null) rebuilt.add(value == entry.getSecond() ? entry : Pair.of(entry.getFirst(), value));
        }
        return rebuilt == null ? node : ops.createMap(rebuilt.stream());
    }

    private T resolveItems(T node, List<T> items, ResourceLocation scope) {
        List<T> rebuilt = null;
        boolean tracking = stack.isEmpty();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (tracking) path.add(i);
            T value = resolve(item, scope);
            if (tracking) path.remove(path.size() - 1);
            if (value != item && rebuilt == null) rebuilt = new ArrayList<>(items.subList(0, i));
            if (rebuilt != null) rebuilt.add(value);
        }
        return rebuilt == null ? node : ops.createList(rebuilt.stream());
    }

    private T call(T node, MapLike<T> map, ResourceLocation scope) {
        if (map.get("macro") == null) {
            error("this macro call has no \"macro\" id. A macro is only defined by its own power file "
                + "or by an entry directly inside apoli:multiple.");
            return node;
        }
        DataResult<MacroUsage> parsed = MacroUsage.CODEC.parse(ops, node);
        MacroUsage usage = parsed.result().orElse(null);
        if (usage == null) {
            error("this macro call could not be read: " + messageOf(parsed));
            return node;
        }
        ResourceLocation id = ResourceLocation.tryParse(IdWildcards.apply(usage.macro(), scope));
        if (id == null) {
            error("\"" + usage.macro() + "\" is not a valid macro id");
            return node;
        }
        String dropped = extraFields(map, CALL_FIELDS);
        if (dropped != null) {
            warning("the call to macro " + id + " drops " + dropped
                + ". A call is replaced by the macro's value, so fields written next to it are not kept.");
        }
        Macro macro = macros.get(id);
        if (macro == null) {
            String reason = broken.get(id);
            error(reason == null ? "macro " + id + " is not defined" : "macro " + id + " could not be loaded: " + reason);
            return node;
        }
        if (stack.contains(id)) {
            error("macro " + id + " ends up calling itself");
            return node;
        }
        if (stack.size() >= MAX_DEPTH) {
            error("macros are nested more than " + MAX_DEPTH + " deep");
            return node;
        }
        Map<String, Dynamic<?>> arguments = arguments(usage.arguments(), scope);
        List<String> parameters = macro.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (!arguments.containsKey(parameters.get(i))) {
                error("macro " + id + " needs the argument [" + parameters.get(i) + "]");
                return node;
            }
        }
        for (String given : arguments.keySet()) {
            if (!parameters.contains(given)) {
                warning("macro " + id + " has no parameter [" + given + "], so that argument is ignored");
            }
        }
        calls++;
        stack.push(id);
        try {
            T body = Placeholders.substitute(ops, macro.value().convert(ops).getValue(), parameters, arguments);
            return resolve(body, macro.scope());
        } finally {
            stack.pop();
        }
    }

    private Map<String, Dynamic<?>> arguments(Map<String, Dynamic<?>> given, ResourceLocation scope) {
        if (given.isEmpty()) return Map.of();
        Map<String, Dynamic<?>> out = new HashMap<>(given.size() * 2);
        boolean tracking = stack.isEmpty();
        for (Map.Entry<String, Dynamic<?>> entry : given.entrySet()) {
            if (tracking) {
                path.add(argumentsKey);
                path.add(ops.createString(entry.getKey()));
            }
            T value = resolve(entry.getValue().convert(ops).getValue(), scope);
            if (tracking) {
                path.remove(path.size() - 1);
                path.remove(path.size() - 1);
            }
            out.put(entry.getKey(), new Dynamic<>(ops, value));
        }
        return out;
    }

    private @Nullable String extraFields(MapLike<T> map, Set<String> allowed) {
        StringBuilder extra = null;
        List<Pair<T, T>> entries = map.entries().toList();
        for (int i = 0; i < entries.size(); i++) {
            String key = ops.getStringValue(entries.get(i).getFirst()).result().orElse(null);
            if (key == null || allowed.contains(key)) continue;
            if (extra == null) extra = new StringBuilder();
            else extra.append(", ");
            extra.append('"').append(key).append('"');
        }
        return extra == null ? null : extra.toString();
    }

    private @Nullable String stringAt(MapLike<T> map, String key) {
        T value = map.get(key);
        return value == null ? null : ops.getStringValue(value).result().orElse(null);
    }

    private void error(String message) {
        report(message, true);
    }

    private void warning(String message) {
        report(message, false);
    }

    private void report(String message, boolean error) {
        StringBuilder line = new StringBuilder(96).append(file == null ? "?" : file.toString());
        if (!path.isEmpty()) line.append(" at ").append(where());
        if (!stack.isEmpty()) line.append(" inside ").append(chain());
        String text = line.append(": ").append(message).toString();
        if (!reported.add(text)) return;
        if (error) {
            errors.add(text);
            LOG.error("[Apoli] Macro error in {}", text);
        } else {
            warnings.add(text);
            LOG.warn("[Apoli] Macro warning in {}", text);
        }
    }

    private String where() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Object segment = path.get(i);
            if (segment instanceof Integer index) {
                out.append('[').append(index).append(']');
                continue;
            }
            @SuppressWarnings("unchecked")
            T key = (T) segment;
            if (!out.isEmpty()) out.append('.');
            out.append(ops.getStringValue(key).result().orElse("?"));
        }
        return out.toString();
    }

    private String chain() {
        StringBuilder out = new StringBuilder();
        Iterator<ResourceLocation> calls = stack.descendingIterator();
        while (calls.hasNext()) {
            if (!out.isEmpty()) out.append(" → ");
            out.append(calls.next());
        }
        return out.toString();
    }

    private static boolean isMultiple(String type) {
        ResourceLocation declared = ResourceLocation.tryParse(type);
        return declared != null && MULTIPLE.equals(PowerTypeRegistry.resolveId(declared));
    }

    private static String messageOf(DataResult<?> result) {
        return result.error().map(error -> error.message()).orElse("unknown error");
    }

    private static final class Overflow extends RuntimeException {
        private Overflow() {
            super(null, null, false, false);
        }
    }
}
