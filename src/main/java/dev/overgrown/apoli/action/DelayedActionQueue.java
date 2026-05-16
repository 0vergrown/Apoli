package dev.overgrown.apoli.action;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class DelayedActionQueue {
    private static final Deque<Entry> QUEUE = new ArrayDeque<>();

    private DelayedActionQueue() {}

    public static synchronized void schedule(int ticks, Runnable action) {
        QUEUE.add(new Entry(ticks, action));
    }

    public static synchronized void tick() {
        if (QUEUE.isEmpty()) return;
        Iterator<Entry> it = QUEUE.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (--e.remaining <= 0) {
                it.remove();
                try { e.action.run(); } catch (Throwable t) {
                    /* swallowed, logged elsewhere */
                }
            }
        }
    }

    private static final class Entry {
        int remaining;
        final Runnable action;
        Entry(int remaining, Runnable action) {
            this.remaining = remaining; this.action = action;
        }
    }
}