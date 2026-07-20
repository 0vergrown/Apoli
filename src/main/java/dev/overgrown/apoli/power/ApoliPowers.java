package dev.overgrown.apoli.power;

import dev.overgrown.apoli.power.builtin.MultiplePower;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ApoliPowers {
    private static volatile Map<ResourceLocation, Power> POWERS = Map.of();
    private static volatile Set<ResourceLocation> SUB_POWERS = Set.of();

    private static volatile int GENERATION = 0;

    private ApoliPowers() {}

    public static int generation() {
        return GENERATION;
    }

    public static @Nullable Power get(ResourceLocation id) {
        return POWERS.get(id);
    }

    public static Map<ResourceLocation, Power> view() {
        return Collections.unmodifiableMap(POWERS);
    }

    public static boolean isSubPower(ResourceLocation id) {
        return SUB_POWERS.contains(id);
    }

    public static Set<ResourceLocation> grantableIds() {
        Set<ResourceLocation> out = new HashSet<>(POWERS.keySet());
        out.removeAll(SUB_POWERS);
        return out;
    }

    public static void replaceAll(Map<ResourceLocation, Power> loaded) {
        POWERS = Map.copyOf(loaded);
        Set<ResourceLocation> subs = new HashSet<>();
        for (Power p : POWERS.values()) {
            if (p.config() instanceof MultiplePower.Cfg cfg) subs.addAll(cfg.subPowerIds());
        }
        SUB_POWERS = Set.copyOf(subs);
        GENERATION++;
    }
}
