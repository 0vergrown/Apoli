package dev.overgrown.apoli.power;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PowerTypeUsage {
    private static final long[] EMPTY = new long[0];
    private static final Map<ResourceLocation, Integer> INDEX = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT = new AtomicInteger();
    private static final Object LOCK = new Object();

    private static volatile long[] held = EMPTY;
    private static long[] sweep = new long[4];
    private static boolean sweeping;

    private PowerTypeUsage() {}

    public static final class Handle {
        private final int word;
        private final long bit;

        private Handle(int index) {
            this.word = index >>> 6;
            this.bit = 1L << index;
        }

        public boolean isHeld() {
            long[] bits = held;
            return word < bits.length && (bits[word] & bit) != 0L;
        }
    }

    public static Handle handle(ResourceLocation canonicalType) {
        return new Handle(indexOf(canonicalType));
    }

    public static boolean isHeld(ResourceLocation canonicalType) {
        Integer index = INDEX.get(canonicalType);
        if (index == null) return false;
        long[] bits = held;
        int word = index >>> 6;
        return word < bits.length && (bits[word] & (1L << index)) != 0L;
    }

    public static long[] bitsOf(Collection<ResourceLocation> canonicalTypes) {
        if (canonicalTypes.isEmpty()) return EMPTY;
        long[] bits = EMPTY;
        for (ResourceLocation type : canonicalTypes) {
            int index = indexOf(type);
            int word = index >>> 6;
            if (word >= bits.length) bits = Arrays.copyOf(bits, word + 1);
            bits[word] |= 1L << index;
        }
        return bits;
    }

    public static void markHeld(ResourceLocation canonicalType) {
        int index = indexOf(canonicalType);
        int word = index >>> 6;
        long bit = 1L << index;
        long[] current = held;
        if (!sweeping && word < current.length && (current[word] & bit) != 0L) return;
        long[] single = new long[word + 1];
        single[word] = bit;
        open(single);
    }

    public static void open(long[] bits) {
        if (bits.length == 0) return;
        synchronized (LOCK) {
            if (sweeping) orInto(bits);
            long[] current = held;
            if (covers(current, bits)) return;
            long[] next = Arrays.copyOf(current, Math.max(current.length, bits.length));
            for (int i = 0; i < bits.length; i++) next[i] |= bits[i];
            held = next;
        }
    }

    public static void beginSweep() {
        synchronized (LOCK) {
            Arrays.fill(sweep, 0L);
            sweeping = true;
        }
    }

    public static void accumulate(long[] bits) {
        if (bits.length == 0) return;
        synchronized (LOCK) {
            orInto(bits);
        }
    }

    public static void endSweep() {
        synchronized (LOCK) {
            sweeping = false;
            long[] current = held;
            int used = sweep.length;
            while (used > 0 && sweep[used - 1] == 0L) used--;
            if (used == trimmedLength(current) && Arrays.equals(current, 0, used, sweep, 0, used)) return;
            held = used == 0 ? EMPTY : Arrays.copyOf(sweep, used);
        }
    }

    public static void reset() {
        synchronized (LOCK) {
            held = EMPTY;
            Arrays.fill(sweep, 0L);
            sweeping = false;
        }
    }

    private static void orInto(long[] bits) {
        if (bits.length > sweep.length) sweep = Arrays.copyOf(sweep, bits.length);
        for (int i = 0; i < bits.length; i++) sweep[i] |= bits[i];
    }

    private static int trimmedLength(long[] bits) {
        int used = bits.length;
        while (used > 0 && bits[used - 1] == 0L) used--;
        return used;
    }

    private static int indexOf(ResourceLocation canonicalType) {
        Integer existing = INDEX.get(canonicalType);
        if (existing != null) return existing;
        return INDEX.computeIfAbsent(canonicalType, k -> NEXT.getAndIncrement());
    }

    private static boolean covers(long[] current, long[] bits) {
        for (int i = 0; i < bits.length; i++) {
            long wanted = bits[i];
            if (wanted == 0L) continue;
            if (i >= current.length || (current[i] & wanted) != wanted) return false;
        }
        return true;
    }
}
