package dev.overgrown.apoli.power.builtin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class LootTableReplacement {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final AtomicInteger ACTIVE = new AtomicInteger();

    private LootTableReplacement() {}

    public static boolean isActive() {
        return ACTIVE.get() > 0;
    }

    public static boolean isStacked(LootTable table) {
        if (!isActive()) return false;
        List<LootTable> marked = STATE.get().marked;
        for (int i = 0; i < marked.size(); i++) {
            if (marked.get(i) == table) return true;
        }
        return false;
    }

    public static void begin(LootTable original, List<LootTable> replacements) {
        State state = STATE.get();
        state.frames.add(state.stack.size());
        state.frames.add(state.marked.size());
        state.frames.add(state.backtrack.size());
        state.stack.add(original);
        state.marked.add(original);
        for (int i = 0; i < replacements.size(); i++) {
            state.stack.add(replacements.get(i));
            state.marked.add(replacements.get(i));
        }
        ACTIVE.incrementAndGet();
    }

    public static void end() {
        State state = STATE.get();
        int last = state.frames.size();
        truncate(state.backtrack, state.frames.removeInt(last - 1));
        truncate(state.marked, state.frames.removeInt(last - 2));
        truncate(state.stack, state.frames.removeInt(last - 3));
        ACTIVE.decrementAndGet();
    }

    public static LootTable peek() {
        if (!isActive()) return LootTable.EMPTY;
        List<LootTable> stack = STATE.get().stack;
        return stack.isEmpty() ? LootTable.EMPTY : stack.get(stack.size() - 1);
    }

    public static void enter() {
        if (!isActive()) return;
        State state = STATE.get();
        state.backtrack.add(state.stack.isEmpty() ? null : state.stack.remove(state.stack.size() - 1));
    }

    public static void exit() {
        if (!isActive()) return;
        State state = STATE.get();
        if (state.backtrack.isEmpty()) return;
        LootTable restored = state.backtrack.remove(state.backtrack.size() - 1);
        if (restored != null) state.stack.add(restored);
    }

    private static void truncate(List<LootTable> list, int size) {
        while (list.size() > size) list.remove(list.size() - 1);
    }

    private static final class State {
        private final List<LootTable> stack = new ArrayList<>();
        private final List<LootTable> marked = new ArrayList<>();
        private final List<LootTable> backtrack = new ArrayList<>();
        private final IntArrayList frames = new IntArrayList();
    }
}
