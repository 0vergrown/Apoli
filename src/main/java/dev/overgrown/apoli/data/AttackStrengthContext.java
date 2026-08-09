package dev.overgrown.apoli.data;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class AttackStrengthContext {

    private static final int NO_ATTACKER = -1;

    public static final long NONE = pack(NO_ATTACKER, 0.0f);

    private static final ThreadLocal<long[]> CURRENT = ThreadLocal.withInitial(() -> new long[] { NONE });

    private AttackStrengthContext() {}

    private static long pack(int attackerId, float scale) {
        return ((long) attackerId << 32) | (Float.floatToRawIntBits(scale) & 0xFFFFFFFFL);
    }

    public static long set(int attackerId, float scale) {
        long[] holder = CURRENT.get();
        long previous = holder[0];
        holder[0] = pack(attackerId, scale);
        return previous;
    }

    public static void restore(long previous) {
        CURRENT.get()[0] = previous;
    }

    public static boolean has(@Nullable Entity attacker) {
        if (attacker == null) return false;
        return (int) (CURRENT.get()[0] >> 32) == attacker.getId();
    }

    public static float scaleFor(@Nullable Entity attacker) {
        if (attacker == null) return 0.0f;
        long current = CURRENT.get()[0];
        if ((int) (current >> 32) != attacker.getId()) return 0.0f;
        return Float.intBitsToFloat((int) current);
    }
}
