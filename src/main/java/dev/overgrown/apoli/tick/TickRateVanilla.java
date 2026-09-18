package dev.overgrown.apoli.tick;

import dev.overgrown.apoli.Apoli;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class TickRateVanilla {
    public static final boolean SUPPORTED = false;

    private static boolean warned;

    private TickRateVanilla() {}

    public static int rate(@Nullable MinecraftServer server) {
        return 20;
    }

    public static float exactRate(@Nullable MinecraftServer server) {
        return 20.0F;
    }

    public static void setRate(MinecraftServer server, float rate) {
        warnUnsupported();
    }

    public static void setFrozen(MinecraftServer server, boolean frozen) {
        warnUnsupported();
    }

    public static void step(MinecraftServer server, int ticks) {
        warnUnsupported();
    }

    public static void sprint(MinecraftServer server, int ticks) {
        warnUnsupported();
    }

    public static boolean frozen(@Nullable MinecraftServer server) {
        return false;
    }

    public static boolean sprinting(@Nullable MinecraftServer server) {
        return false;
    }

    public static boolean stepping(@Nullable MinecraftServer server) {
        return false;
    }

    private static void warnUnsupported() {
        if (warned) return;
        warned = true;
        Apoli.LOGGER.warn("[Apoli] apoli:tick_rate with scope 'server' needs the /tick command, which "
            + "Minecraft 1.20.1 does not have. Use scope 'dimension' instead. This is logged once.");
    }
}
