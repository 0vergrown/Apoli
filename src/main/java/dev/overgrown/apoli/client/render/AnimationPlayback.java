package dev.overgrown.apoli.client.render;

import dev.overgrown.apoli.data.ModelAnimation;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AnimationPlayback {

    private static final int SWEEP_INTERVAL = 1200;
    private static final int STALE_AFTER = 100;

    private static final Map<Long, State> ACTIVE = new HashMap<>();
    private static int nextSweep;

    private AnimationPlayback() {}

    public static float elapsed(int entityId, ResourceLocation model, ModelAnimation.Entry entry,
                                int tickCount, float partialTick) {
        return elapsedSince(startTick(entityId, model, entry, tickCount), tickCount, partialTick, entry.speed());
    }

    public static int startTick(int entityId, ResourceLocation model, ModelAnimation.Entry entry, int tickCount) {
        long slot = ((long) entityId << 32) | (model.hashCode() & 0xFFFFFFFFL);
        int key = entry.key();
        State state = ACTIVE.get(slot);
        if (state == null) {
            state = new State(key, tickCount);
            ACTIVE.put(slot, state);
        } else if (state.key != key || tickCount < state.startTick) {
            state.key = key;
            state.startTick = tickCount;
        }
        sweep(tickCount);
        state.lastSeen = tickCount;
        return state.startTick;
    }

    public static float elapsedSince(int startTick, int tickCount, float partialTick, float speed) {
        return ((tickCount - startTick) + partialTick) / 20.0F * speed;
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void sweep(int now) {
        if (now < nextSweep) return;
        nextSweep = now + SWEEP_INTERVAL;
        Iterator<Map.Entry<Long, State>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next().getValue();
            if (now - state.lastSeen > STALE_AFTER || state.lastSeen > now) iterator.remove();
        }
    }

    private static final class State {
        private int key;
        private int startTick;
        private int lastSeen;

        private State(int key, int startTick) {
            this.key = key;
            this.startTick = startTick;
            this.lastSeen = startTick;
        }
    }
}
