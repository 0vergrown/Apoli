package dev.overgrown.apoli.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

public final class CopyOnWriteIntMap<V> {
    private volatile Int2ObjectOpenHashMap<V> map = new Int2ObjectOpenHashMap<>();

    public @Nullable V get(int key) {
        return this.map.get(key);
    }

    public V getOrDefault(int key, V fallback) {
        V value = this.map.get(key);
        return value == null ? fallback : value;
    }

    public boolean containsKey(int key) {
        return this.map.containsKey(key);
    }

    public synchronized void put(int key, V value) {
        Int2ObjectOpenHashMap<V> next = new Int2ObjectOpenHashMap<>(this.map);
        next.put(key, value);
        this.map = next;
    }

    public synchronized void remove(int key) {
        if (!this.map.containsKey(key)) return;
        Int2ObjectOpenHashMap<V> next = new Int2ObjectOpenHashMap<>(this.map);
        next.remove(key);
        this.map = next;
    }

    public synchronized void clear() {
        if (this.map.isEmpty()) return;
        this.map = new Int2ObjectOpenHashMap<>();
    }

    public Int2ObjectMap<V> snapshot() {
        return Int2ObjectMaps.unmodifiable(this.map);
    }
}
