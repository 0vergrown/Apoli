package dev.overgrown.apoli.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.Supplier;

public final class LazyCodec<A> implements Codec<A> {
    private final Supplier<Codec<A>> supplier;
    private volatile Codec<A> delegate;

    public LazyCodec(Supplier<Codec<A>> supplier) {
        this.supplier = supplier;
    }

    private Codec<A> get() {
        Codec<A> c = delegate;
        if (c == null) {
            c = supplier.get();
            delegate = c;
        }
        return c;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return get().decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return get().encode(input, ops, prefix);
    }
}
