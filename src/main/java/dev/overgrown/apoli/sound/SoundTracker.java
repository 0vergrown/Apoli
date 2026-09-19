package dev.overgrown.apoli.sound;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SoundTracker {

    public static final int UNKNOWN_RANGE = -1;

    private static final int CAPACITY = 64;

    private static final Set<ResourceLocation> WATCHED = ConcurrentHashMap.newKeySet();

    private static volatile boolean watching;

    private static final Log SERVER = new Log();

    private static final Log CLIENT = new Log();

    private SoundTracker() {}

    public static boolean watching() {
        return watching;
    }

    public static void watch(ResourceLocation sound) {
        if (WATCHED.add(sound)) watching = true;
    }

    public static boolean watched(ResourceLocation sound) {
        return WATCHED.contains(sound);
    }

    public static void record(Level level, ResourceLocation sound, SoundSource source,
                              double x, double y, double z, float range) {
        if (!watching || !WATCHED.contains(sound)) return;
        Log log = level.isClientSide() ? CLIENT : SERVER;
        int slot = log.next;
        log.sounds[slot] = sound;
        log.sources[slot] = source;
        log.dimensions[slot] = level.dimension();
        log.x[slot] = x;
        log.y[slot] = y;
        log.z[slot] = z;
        log.ranges[slot] = range;
        log.times[slot] = level.getGameTime();
        log.next = slot + 1 == CAPACITY ? 0 : slot + 1;
    }

    public static boolean playing(Entity listener, ResourceLocation sound, @Nullable SoundSource source,
                                  int duration, double range) {
        if (!watching) return false;
        Level level = listener.level();
        Log log = level.isClientSide() ? CLIENT : SERVER;
        ResourceKey<Level> dimension = level.dimension();
        long now = level.getGameTime();
        double ex = listener.getX();
        double ey = listener.getEyeY();
        double ez = listener.getZ();
        boolean explicitRange = range >= 0.0;
        double explicitSquared = range * range;

        for (int i = 0; i < CAPACITY; i++) {
            ResourceLocation candidate = log.sounds[i];
            if (candidate == null || !candidate.equals(sound)) continue;
            if (log.dimensions[i] != dimension) continue;
            long elapsed = now - log.times[i];
            if (elapsed < 0 || elapsed >= duration) continue;
            if (source != null && log.sources[i] != source) continue;
            double limitSquared;
            if (explicitRange) {
                limitSquared = explicitSquared;
            } else if (log.ranges[i] < 0.0F) {
                return true;
            } else {
                limitSquared = log.ranges[i] * (double) log.ranges[i];
            }
            double dx = log.x[i] - ex;
            double dy = log.y[i] - ey;
            double dz = log.z[i] - ez;
            if (dx * dx + dy * dy + dz * dz <= limitSquared) return true;
        }
        return false;
    }

    private static final class Log {
        private final ResourceLocation[] sounds = new ResourceLocation[CAPACITY];
        private final SoundSource[] sources = new SoundSource[CAPACITY];
        @SuppressWarnings("unchecked")
        private final ResourceKey<Level>[] dimensions = new ResourceKey[CAPACITY];
        private final double[] x = new double[CAPACITY];
        private final double[] y = new double[CAPACITY];
        private final double[] z = new double[CAPACITY];
        private final float[] ranges = new float[CAPACITY];
        private final long[] times = new long[CAPACITY];
        private int next;
    }
}
