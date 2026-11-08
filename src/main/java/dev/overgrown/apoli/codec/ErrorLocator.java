package dev.overgrown.apoli.codec;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import net.minecraft.resources.DelegatingOps;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ErrorLocator {

    private static final ThreadLocal<Boolean> LOCATING = new ThreadLocal<>();
    private static final int MAX_EVENTS = 8192;

    private ErrorLocator() {}

    public static boolean locating() {
        return LOCATING.get() != null;
    }

    public static @Nullable String locate(Decoder<?> decoder, JsonElement json, String message) {
        Tracker tracker = new Tracker();
        tracker.paths.put(json, "$");
        Boolean previous = LOCATING.get();
        LOCATING.set(Boolean.TRUE);
        try {
            decoder.parse(new Dynamic<>(wrap(new TrackingOps(JsonOps.INSTANCE, tracker)), json));
        } catch (RuntimeException ignored) {
        } finally {
            if (previous == null) LOCATING.remove();
        }
        return tracker.best(tidy(message));
    }

    private static DynamicOps<JsonElement> wrap(DynamicOps<JsonElement> tracking) {
        return ApoliOps.of(tracking);
    }

    public static String tidy(String message) {
        if (message == null || message.isEmpty()) return "unknown error";
        StringBuilder out = new StringBuilder(Math.min(message.length(), 512));
        int i = 0;
        int n = message.length();
        while (i < n) {
            if (message.startsWith(" in MapLike[", i)) {
                int close = closing(message, i + 11);
                if (close > 0) {
                    i = close + 1;
                    continue;
                }
            }
            if (message.startsWith("MapLike[", i)) {
                int close = closing(message, i + 7);
                if (close > 0) {
                    out.append("an object");
                    i = close + 1;
                    continue;
                }
            }
            char c = message.charAt(i);
            if ((c == '{' || c == '[') && i + 1 < n && (message.charAt(i + 1) == '"' || message.charAt(i + 1) == '{')) {
                int close = closing(message, i);
                if (close > 0 && close - i > 80) {
                    out.append(c).append("...").append(message.charAt(close));
                    i = close + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return OBJECT_NAME.matcher(out.toString()).replaceAll("").trim();
    }

    private static final java.util.regex.Pattern OBJECT_NAME = java.util.regex.Pattern.compile(" in [A-Za-z_$][\\w.$]*@[0-9a-f]+");

    private static int closing(String text, int open) {
        char start = text.charAt(open);
        char stop = start == '{' ? '}' : ']';
        int depth = 0;
        boolean quoted = false;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0) return c == stop ? i : -1;
            }
        }
        return -1;
    }

    private static final class Tracker {
        private final IdentityHashMap<JsonElement, String> paths = new IdentityHashMap<>();
        private final List<String> failPaths = new ArrayList<>();
        private final List<String[]> failNeedles = new ArrayList<>();
        private final List<String> valuePaths = new ArrayList<>();
        private final List<String> values = new ArrayList<>();

        private void child(JsonElement parent, String segment, @Nullable JsonElement child) {
            if (child == null) return;
            String base = paths.get(parent);
            if (base != null) paths.putIfAbsent(child, base + segment);
        }

        private void fail(@Nullable String path, String... needles) {
            if (path == null || failPaths.size() >= MAX_EVENTS) return;
            failPaths.add(path);
            failNeedles.add(needles);
        }

        private void failed(JsonElement input, DataResult<?> result) {
            String path = paths.get(input);
            if (path == null) return;
            result.error().ifPresent(error -> fail(path, tidy(error.message())));
        }

        private void value(JsonElement input, String value) {
            String path = paths.get(input);
            if (path == null || value.length() < 2 || values.size() >= MAX_EVENTS) return;
            valuePaths.add(path);
            values.add(value);
        }

        private @Nullable String best(String message) {
            String best = null;
            for (int i = 0; i < failPaths.size(); i++) {
                String path = failPaths.get(i);
                if (best != null && path.length() < best.length()) continue;
                for (String needle : failNeedles.get(i)) {
                    if (!needle.isEmpty() && message.contains(needle)) {
                        best = path;
                        break;
                    }
                }
            }
            if (best != null) return best;
            int bestValue = -1;
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                if (!message.contains(value)) continue;
                if (best == null || value.length() > bestValue
                    || value.length() == bestValue && valuePaths.get(i).length() >= best.length()) {
                    best = valuePaths.get(i);
                    bestValue = value.length();
                }
            }
            if (best != null) return best;
            int at = 0;
            while ((at = message.indexOf("The '", at)) >= 0) {
                int close = message.indexOf('\'', at + 5);
                if (close < 0) break;
                String suffix = "." + message.substring(at + 5, close);
                for (String path : paths.values()) {
                    if (path.endsWith(suffix) && (best == null || path.length() > best.length())) best = path;
                }
                at = close;
            }
            return best;
        }
    }

    private static final class TrackingOps extends DelegatingOps<JsonElement> {
        private final Tracker tracker;

        private TrackingOps(DynamicOps<JsonElement> delegate, Tracker tracker) {
            super(delegate);
            this.tracker = tracker;
        }

        @Override
        public DataResult<Number> getNumberValue(JsonElement input) {
            DataResult<Number> result = super.getNumberValue(input);
            if (result.result().isEmpty()) tracker.failed(input, result);
            return result;
        }

        @Override
        public DataResult<Boolean> getBooleanValue(JsonElement input) {
            DataResult<Boolean> result = super.getBooleanValue(input);
            if (result.result().isEmpty()) tracker.failed(input, result);
            return result;
        }

        @Override
        public DataResult<String> getStringValue(JsonElement input) {
            DataResult<String> result = super.getStringValue(input);
            String value = result.result().orElse(null);
            if (value == null) {
                tracker.failed(input, result);
            } else {
                tracker.value(input, value);
            }
            return result;
        }

        @Override
        public DataResult<MapLike<JsonElement>> getMap(JsonElement input) {
            DataResult<MapLike<JsonElement>> result = super.getMap(input);
            MapLike<JsonElement> map = result.result().orElse(null);
            if (map == null) {
                tracker.failed(input, result);
                return result;
            }
            return DataResult.success(new TrackedMap(input, map));
        }

        @Override
        public DataResult<Stream<Pair<JsonElement, JsonElement>>> getMapValues(JsonElement input) {
            DataResult<Stream<Pair<JsonElement, JsonElement>>> result = super.getMapValues(input);
            if (result.result().isEmpty()) {
                tracker.failed(input, result);
                return result;
            }
            return result.map(stream -> stream.peek(pair -> track(input, pair.getFirst(), pair.getSecond())));
        }

        @Override
        public DataResult<Consumer<BiConsumer<JsonElement, JsonElement>>> getMapEntries(JsonElement input) {
            DataResult<Consumer<BiConsumer<JsonElement, JsonElement>>> result = super.getMapEntries(input);
            if (result.result().isEmpty()) {
                tracker.failed(input, result);
                return result;
            }
            return result.map(entries -> sink -> entries.accept((key, value) -> {
                track(input, key, value);
                sink.accept(key, value);
            }));
        }

        @Override
        public DataResult<Stream<JsonElement>> getStream(JsonElement input) {
            DataResult<Stream<JsonElement>> result = super.getStream(input);
            if (result.result().isEmpty()) {
                tracker.failed(input, result);
                return result;
            }
            int[] index = new int[1];
            return result.map(stream -> stream.peek(element -> tracker.child(input, "[" + index[0]++ + "]", element)));
        }

        @Override
        public DataResult<Consumer<Consumer<JsonElement>>> getList(JsonElement input) {
            DataResult<Consumer<Consumer<JsonElement>>> result = super.getList(input);
            if (result.result().isEmpty()) {
                tracker.failed(input, result);
                return result;
            }
            return result.map(list -> sink -> {
                int[] index = new int[1];
                list.accept(element -> {
                    tracker.child(input, "[" + index[0]++ + "]", element);
                    sink.accept(element);
                });
            });
        }

        private void track(JsonElement parent, @Nullable JsonElement key, @Nullable JsonElement value) {
            if (key == null || value == null) return;
            String name = key.isJsonPrimitive() ? key.getAsString() : null;
            if (name != null) tracker.child(parent, "." + name, value);
        }

        private final class TrackedMap implements MapLike<JsonElement> {
            private final JsonElement owner;
            private final MapLike<JsonElement> map;

            private TrackedMap(JsonElement owner, MapLike<JsonElement> map) {
                this.owner = owner;
                this.map = map;
            }

            @Override
            public @Nullable JsonElement get(JsonElement key) {
                JsonElement value = map.get(key);
                String name = key != null && key.isJsonPrimitive() ? key.getAsString() : null;
                if (name != null) found(name, value);
                return value;
            }

            @Override
            public @Nullable JsonElement get(String key) {
                JsonElement value = map.get(key);
                found(key, value);
                return value;
            }

            private void found(String name, @Nullable JsonElement value) {
                if (value != null) {
                    tracker.child(owner, "." + name, value);
                    return;
                }
                String path = tracker.paths.get(owner);
                if (path != null) tracker.fail(path + "." + name, "No key " + name, "No element " + name, "key [" + name + "]");
            }

            @Override
            public Stream<Pair<JsonElement, JsonElement>> entries() {
                return map.entries().peek(pair -> track(owner, pair.getFirst(), pair.getSecond()));
            }

            @Override
            public String toString() {
                return map.toString();
            }
        }
    }
}
