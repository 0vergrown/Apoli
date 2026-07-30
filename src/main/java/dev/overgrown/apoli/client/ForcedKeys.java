package dev.overgrown.apoli.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class ForcedKeys {
    private static final class Entry {
        int ticks;
        int clicks;
    }

    private static final Map<String, Entry> BY_NAME = new HashMap<>();
    private static Map<KeyMapping, Entry> resolved = Map.of();
    private static boolean any;
    private static boolean needsResolve;

    private ForcedKeys() {}

    public static void force(String key, int duration, boolean release) {
        if (key == null || key.isEmpty()) return;
        if (release) {
            if (BY_NAME.remove(key) == null) return;
        } else {
            Entry entry = BY_NAME.get(key);
            if (entry == null) {
                entry = new Entry();
                BY_NAME.put(key, entry);
            }
            int ticks = Math.max(1, duration);
            if (ticks > entry.ticks) entry.ticks = ticks;
            entry.clicks++;
        }
        needsResolve = true;
        rebuild();
    }

    public static void tick() {
        if (!any && !needsResolve) return;
        boolean expired = false;
        for (Iterator<Map.Entry<String, Entry>> it = BY_NAME.entrySet().iterator(); it.hasNext(); ) {
            Entry entry = it.next().getValue();
            entry.ticks--;
            if (entry.ticks <= 0) {
                it.remove();
                expired = true;
            }
        }
        if (expired || needsResolve) rebuild();
    }

    public static void clear() {
        BY_NAME.clear();
        resolved = Map.of();
        any = false;
        needsResolve = false;
    }

    public static boolean isForced(KeyMapping mapping) {
        return any && resolved.containsKey(mapping);
    }

    public static boolean consumeForcedClick(KeyMapping mapping) {
        if (!any) return false;
        Entry entry = resolved.get(mapping);
        if (entry == null || entry.clicks <= 0) return false;
        entry.clicks--;
        return true;
    }

    private static void rebuild() {
        if (BY_NAME.isEmpty()) {
            resolved = Map.of();
            any = false;
            needsResolve = false;
            return;
        }
        Map<KeyMapping, Entry> next = new IdentityHashMap<>(BY_NAME.size());
        boolean unresolved = false;
        for (Map.Entry<String, Entry> entry : BY_NAME.entrySet()) {
            KeyMapping mapping = ApoliKeyMappings.resolve(entry.getKey());
            if (mapping == null) {
                unresolved = true;
                continue;
            }
            next.put(mapping, entry.getValue());
        }
        resolved = next;
        any = !next.isEmpty();
        needsResolve = unresolved;
    }
}
