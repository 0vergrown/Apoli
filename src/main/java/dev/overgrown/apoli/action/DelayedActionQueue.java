package dev.overgrown.apoli.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DelayedActionQueue {

    private static long currentTick;

    private static final Map<Long, List<Runnable>> BUCKETS = new HashMap<>();

    private DelayedActionQueue() {}

    public static synchronized void schedule(int ticks, Runnable action) {
        long fireTick = currentTick + Math.max(1, ticks);
        BUCKETS.computeIfAbsent(fireTick, k -> new ArrayList<>()).add(action);
    }

    public static synchronized void tick() {
        List<Runnable> due = BUCKETS.remove(++currentTick);
        if (due == null) return;
        for (int i = 0; i < due.size(); i++) {
            try {
                due.get(i).run();
            } catch (Throwable t) {
            }
        }
    }

    public static synchronized void clear() {
        BUCKETS.clear();
        currentTick = 0;
    }
}
