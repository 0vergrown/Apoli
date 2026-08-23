package dev.overgrown.apoli.loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IdWildcards {
    public static final String SELF_ID = "*:*";
    private static final String SELF_NAMESPACE = "*:";
    private static final String TAGGED_SELF_NAMESPACE = "#*:";

    private IdWildcards() {}

    public static <T> Dynamic<T> apply(Dynamic<T> data, ResourceLocation self) {
        return walk(data, self.getNamespace(), self.getNamespace() + ":" + self.getPath());
    }

    /** Convenience bridge for callers that already hold a Gson tree, such as the Origins loaders. */
    public static JsonElement apply(JsonElement json, ResourceLocation self) {
        return apply(new Dynamic<>(JsonOps.INSTANCE, json), self).getValue();
    }

    public static String apply(String value, ResourceLocation self) {
        return expand(value, self.getNamespace(), self.getNamespace() + ":" + self.getPath());
    }

    private static <T> Dynamic<T> walk(Dynamic<T> data, String namespace, String fullId) {
        Map<Dynamic<T>, Dynamic<T>> entries = data.getMapValues().result().orElse(null);
        if (entries != null) {
            Map<Dynamic<T>, Dynamic<T>> rebuilt = new LinkedHashMap<>(entries.size());
            for (Map.Entry<Dynamic<T>, Dynamic<T>> entry : entries.entrySet()) {
                rebuilt.put(entry.getKey(), walk(entry.getValue(), namespace, fullId));
            }
            return data.createMap(rebuilt);
        }

        var list = data.asStreamOpt().result().orElse(null);
        if (list != null) {
            return data.createList(list.map(element -> walk(element, namespace, fullId)));
        }

        String raw = data.asString().result().orElse(null);
        if (raw == null) return data;
        String expanded = expand(raw, namespace, fullId);
        return expanded.equals(raw) ? data : data.createString(expanded);
    }

    private static String expand(String value, String namespace, String fullId) {
        if (value.indexOf('*') < 0) return value;
        String out = value.contains(SELF_ID) ? value.replace(SELF_ID, fullId) : value;
        if (out.startsWith(SELF_NAMESPACE)) {
            return namespace + ':' + out.substring(SELF_NAMESPACE.length());
        }
        if (out.startsWith(TAGGED_SELF_NAMESPACE)) {
            return '#' + namespace + ':' + out.substring(TAGGED_SELF_NAMESPACE.length());
        }
        return expandInline(out, namespace);
    }

    private static String expandInline(String value, String namespace) {
        int at = value.indexOf(SELF_NAMESPACE);
        if (at < 0) return value;
        StringBuilder out = null;
        int copied = 0;
        while (at >= 0) {
            if (standsAlone(value, at)) {
                if (out == null) out = new StringBuilder(value.length() + namespace.length());
                out.append(value, copied, at).append(namespace).append(':');
                copied = at + SELF_NAMESPACE.length();
            }
            at = value.indexOf(SELF_NAMESPACE, at + 1);
        }
        if (out == null) return value;
        return out.append(value, copied, value.length()).toString();
    }

    private static boolean standsAlone(String value, int at) {
        int after = at + SELF_NAMESPACE.length();
        if (after >= value.length() || !isPathChar(value.charAt(after))) return false;
        if (at == 0) return true;
        char before = value.charAt(at - 1);
        return !Character.isLetterOrDigit(before) && before != '_' && before != '.' && before != ':';
    }

    private static boolean isPathChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '/' || c == '-';
    }
}
