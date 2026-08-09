package dev.overgrown.apoli.action;

import dev.overgrown.apoli.Apoli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class DelayedActionQueue {

    public static final BooleanSupplier ALWAYS = () -> true;

    private static long currentTick;

    private static boolean loggedActionFailure;

    private static final Map<Long, List<Entry>> BUCKETS = new HashMap<>();

    private record Entry(BooleanSupplier alive, Runnable action) {}

    private DelayedActionQueue() {}

    public static synchronized void schedule(int ticks, BooleanSupplier alive, Runnable action) {
        long fireTick = currentTick + Math.max(1, ticks);
        BUCKETS.computeIfAbsent(fireTick, k -> new ArrayList<>()).add(new Entry(alive, action));
    }

    public static synchronized void tick() {
        List<Entry> due = BUCKETS.remove(++currentTick);
        if (due == null) return;
        for (int i = 0; i < due.size(); i++) {
            Entry entry = due.get(i);
            try {
                if (!entry.alive().getAsBoolean()) continue;
                entry.action().run();
            } catch (Throwable t) {
                if (!loggedActionFailure) {
                    loggedActionFailure = true;
                    Apoli.LOGGER.error("[Apoli] A delayed action (apoli:delay / apoli:loop) threw; that chain "
                        + "was dropped. Later failures are not logged.", t);
                }
            }
        }
    }

    public static synchronized void clear() {
        BUCKETS.clear();
        currentTick = 0;
        loggedActionFailure = false;
    }
}
