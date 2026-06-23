package dev.overgrown.apoli.alias;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Map;
import java.util.stream.Stream;

public final class DefaultInjectingMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> delegate;
    private final Map<String, String> defaults;

    private DefaultInjectingMapCodec(MapCodec<A> delegate, Map<String, String> defaults) {
        this.delegate = delegate;
        this.defaults = Map.copyOf(defaults);
    }

    public static <A> MapCodec<A> wrap(MapCodec<A> delegate, Map<String, String> defaults) {
        return defaults == null || defaults.isEmpty() ? delegate : new DefaultInjectingMapCodec<>(delegate, defaults);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return delegate.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        return delegate.decode(ops, new DefaultingMapLike<>(ops, input, defaults));
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return delegate.encode(input, ops, prefix);
    }

    private record DefaultingMapLike<T>(DynamicOps<T> ops, MapLike<T> wrapped, Map<String, String> defaults)
        implements MapLike<T> {
        @Override
        public T get(T key) {
            T direct = wrapped.get(key);
            if (direct != null) return direct;
            return ops.getStringValue(key).result().map(this::byName).orElse(null);
        }

        @Override
        public T get(String key) {
            T direct = wrapped.get(key);
            return direct != null ? direct : byName(key);
        }

        private T byName(String key) {
            String def = defaults.get(key);
            return def != null ? ops.createString(def) : null;
        }

        @Override
        public Stream<Pair<T, T>> entries() {
            return wrapped.entries();
        }
    }
}
