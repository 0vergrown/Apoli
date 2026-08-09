package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.power.ApoliPowers;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FunctionWarnings {

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static volatile int generation = -1;

    private FunctionWarnings() {}

    public static boolean first(ResourceLocation function, String kind) {
        int current = ApoliPowers.generation();
        if (current != generation) {
            generation = current;
            SEEN.clear();
        }
        return SEEN.add(kind + '\0' + function);
    }
}
