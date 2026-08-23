package dev.overgrown.apoli.data;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class CriticalHitContext {

    private static final int NO_ATTACKER = -1;

    public static final long NONE = pack(NO_ATTACKER, false);

    private static final ThreadLocal<long[]> CURRENT = ThreadLocal.withInitial(() -> new long[] { NONE });

    private CriticalHitContext() {}

    private static long pack(int attackerId, boolean critical) {
        return ((long) attackerId << 32) | (critical ? 1L : 0L);
    }

    public static long set(int attackerId, boolean critical) {
        long[] holder = CURRENT.get();
        long previous = holder[0];
        holder[0] = pack(attackerId, critical);
        return previous;
    }

    public static void restore(long previous) {
        CURRENT.get()[0] = previous;
    }

    public static boolean isCritical(@Nullable Entity attacker) {
        if (attacker == null) return false;
        long current = CURRENT.get()[0];
        if ((int) (current >> 32) != attacker.getId()) return false;
        return (current & 1L) != 0L;
    }
}
