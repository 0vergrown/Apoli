package dev.overgrown.apoli.client;

import dev.overgrown.apoli.mixin.keybinding.KeyMappingClickCountAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.KeyMapping;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ForcedKeys {
    private static final class Entry {
        int ticks;
        int clicks;
        int localClicks;
    }

    private static final Map<String, Entry> BY_NAME = new HashMap<>();
    private static final Map<KeyMapping, Integer> RELEASED = new IdentityHashMap<>();
    private static Map<KeyMapping, Entry> resolved = Map.of();
    private static boolean any;
    private static boolean anyReleased;
    private static boolean needsResolve;

    private ForcedKeys() {}

    public static void force(String key, int duration, boolean release) {
        if (key == null || key.isEmpty()) return;
        if (release) {
            BY_NAME.remove(key);
            letGo(ApoliKeyMappings.resolve(key));
        } else {
            Entry entry = BY_NAME.get(key);
            if (entry == null) {
                entry = new Entry();
                BY_NAME.put(key, entry);
            }
            int ticks = Math.max(1, duration);
            if (ticks > entry.ticks) entry.ticks = ticks;
            entry.clicks++;
            entry.localClicks++;
            if (anyReleased) {
                RELEASED.remove(ApoliKeyMappings.resolve(key));
                anyReleased = !RELEASED.isEmpty();
            }
        }
        needsResolve = true;
        rebuild();
    }

    private static void letGo(KeyMapping mapping) {
        if (mapping == null) return;
        boolean held = ApoliKeyMappings.isHeld(mapping);
        drop(mapping);
        if (!held) return;
        RELEASED.put(mapping, ApoliKeyMappings.releases(mapping));
        anyReleased = true;
    }

    private static void drop(KeyMapping mapping) {
        mapping.setDown(false);
        ((KeyMappingClickCountAccessor) mapping).apoli$setClickCount(0);
    }

    public static void tick() {
        if (anyReleased) tickReleased();
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

    private static void tickReleased() {
        for (Iterator<Map.Entry<KeyMapping, Integer>> it = RELEASED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<KeyMapping, Integer> entry = it.next();
            KeyMapping mapping = entry.getKey();
            boolean held = ApoliKeyMappings.isHeld(mapping);
            if (held && ApoliKeyMappings.releases(mapping) == entry.getValue()) continue;
            it.remove();
            if (!held) {
                drop(mapping);
                continue;
            }
            KeyMappingClickCountAccessor clicks = (KeyMappingClickCountAccessor) mapping;
            if (clicks.apoli$getClickCount() > 1) clicks.apoli$setClickCount(1);
        }
        anyReleased = !RELEASED.isEmpty();
    }

    public static void clear() {
        BY_NAME.clear();
        RELEASED.clear();
        resolved = Map.of();
        any = false;
        anyReleased = false;
        needsResolve = false;
    }

    public static boolean isForced(KeyMapping mapping) {
        return any && resolved.containsKey(mapping);
    }

    public static boolean isReleased(KeyMapping mapping) {
        return anyReleased && RELEASED.containsKey(mapping);
    }

    public static boolean consumeForcedClick(KeyMapping mapping) {
        if (!any) return false;
        Entry entry = resolved.get(mapping);
        if (entry == null || entry.clicks <= 0) return false;
        entry.clicks--;
        return true;
    }

    public static boolean consumeLocalForcedClick(KeyMapping mapping) {
        if (!any) return false;
        Entry entry = resolved.get(mapping);
        if (entry == null || entry.localClicks <= 0) return false;
        entry.localClicks--;
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
