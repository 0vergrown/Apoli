package dev.overgrown.apoli.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Map;

public final class IdWildcards {
    public static final String SELF_ID = "*:*";
    private static final String SELF_NAMESPACE = "*:";
    private static final String TAGGED_SELF_NAMESPACE = "#*:";

    private IdWildcards() {}

    public static JsonElement apply(JsonElement json, ResourceLocation self) {
        return walk(json, self.getNamespace(), self.getNamespace() + ":" + self.getPath());
    }

    public static String apply(String value, ResourceLocation self) {
        return expand(value, self.getNamespace(), self.getNamespace() + ":" + self.getPath());
    }

    private static JsonElement walk(JsonElement json, String namespace, String fullId) {
        if (json instanceof JsonObject obj) {
            for (Map.Entry<String, JsonElement> e : new ArrayList<>(obj.entrySet())) {
                obj.add(e.getKey(), walk(e.getValue(), namespace, fullId));
            }
            return obj;
        }
        if (json instanceof JsonArray arr) {
            for (int i = 0; i < arr.size(); i++) arr.set(i, walk(arr.get(i), namespace, fullId));
            return arr;
        }
        if (json instanceof JsonPrimitive p && p.isString()) {
            String raw = p.getAsString();
            String expanded = expand(raw, namespace, fullId);
            if (!expanded.equals(raw)) return new JsonPrimitive(expanded);
        }
        return json;
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
        return out;
    }
}
