package dev.overgrown.apoli.dev;

import dev.overgrown.apoli.data.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DevParticles {

    public enum Ray {
        ACTION(new Vector3f(1.0F, 0.2F, 0.2F)),
        CONDITION(new Vector3f(1.0F, 0.85F, 0.15F));

        private final DustParticleOptions solid;
        private final DustParticleOptions faint;

        Ray(Vector3f colour) {
            this.solid = new DustParticleOptions(colour, 1.0F);
            this.faint = new DustParticleOptions(colour, 0.65F);
        }
    }

    private static final DustParticleOptions BLUE =
        new DustParticleOptions(new Vector3f(0.25F, 0.55F, 1.0F), 0.75F);
    private static final DustParticleOptions GREEN =
        new DustParticleOptions(new Vector3f(0.35F, 0.95F, 0.4F), 0.75F);

    private static final int RING_STEPS = 48;
    private static final int SLANTS = 8;
    private static final int MAX_POINTS = 600;
    private static final int RAY_POINTS = 1200;
    private static final int TICK_BUDGET = 4000;
    private static final int REFRESH_PERIOD = 10;
    private static final double SHAPE_DENSITY = 2.0;
    private static final double RAY_DENSITY = 4.0;
    private static final double RAY_STEP = 0.25;
    private static final double REACH_STEP = 0.6;
    private static final double LEAD = 0.5;
    private static final double MARK_RADIUS = 0.2;
    private static final double BLOCK_HALF = 0.51;
    private static final int MAX_TRACKED = 4096;

    private static long budgetTick = Long.MIN_VALUE;
    private static int tickBudget;
    private static final Map<Drawn, Long> LAST_DRAWN = new HashMap<>();

    private DevParticles() {}

    public static boolean due(Level level) {
        return DevMode.any() && level.getGameTime() % REFRESH_PERIOD == 0;
    }

    public static boolean due(Level level, Entity subject, Object config) {
        return DevMode.any() && dueFor(level, subject.getId(), -1, config);
    }

    public static boolean due(Level level, Entity actor, Entity target, Object config) {
        return DevMode.any() && dueFor(level, actor.getId(), target.getId(), config);
    }

    static void forget() {
        LAST_DRAWN.clear();
    }

    private static boolean dueFor(Level level, int first, int second, Object config) {
        if (!(level instanceof ServerLevel)) return false;
        long now = level.getGameTime();
        Drawn key = new Drawn(first, second, config);
        Long last = LAST_DRAWN.get(key);
        if (last != null && now >= last && now - last < REFRESH_PERIOD) return false;
        if (LAST_DRAWN.size() >= MAX_TRACKED) LAST_DRAWN.clear();
        LAST_DRAWN.put(key, now);
        return true;
    }

    public static void outlineShape(ServerLevel level, Vec3 origin, Shape shape, double rx, double ry, double rz) {
        emitShape(level, origin, shape, rx, ry, rz, BLUE);
    }

    public static void outlineCondition(ServerLevel level, Vec3 origin, Shape shape, double rx, double ry, double rz) {
        emitShape(level, origin, shape, rx, ry, rz, GREEN);
    }

    public static void outlineBox(ServerLevel level, AABB box) {
        emitShape(level, box.getCenter(), Shape.CUBE,
            box.getXsize() * 0.5, box.getYsize() * 0.5, box.getZsize() * 0.5, BLUE);
    }

    private static void emitShape(ServerLevel level, Vec3 origin, Shape shape,
                                  double rx, double ry, double rz, DustParticleOptions color) {
        Emitter out = emitter(level, color, MAX_POINTS);
        if (out == null) return;
        switch (shape) {
            case CUBE -> box(out, origin, rx, ry, rz, SHAPE_DENSITY);
            case SPHERE -> ellipsoid(out, origin, rx, ry, rz);
            case STAR -> star(out, origin, rx, ry, rz);
            case CONE -> cone(out, origin, rx, ry, rz);
        }
    }

    public static void ray(ServerLevel level, Ray kind, Vec3 origin, Vec3 dir, double stop, double reach) {
        List<ServerPlayer> watchers = DevMode.watchers(level);
        if (watchers.isEmpty()) return;
        double end = Math.max(stop, reach);
        trace(level, watchers, kind, origin, dir, stop, end);
        ring(new Emitter(level, watchers, kind.faint, RAY_POINTS), origin.add(dir.scale(end)), dir, MARK_RADIUS);
    }

    public static void segment(ServerLevel level, Ray kind, Vec3 from, Vec3 to, double stop) {
        List<ServerPlayer> watchers = DevMode.watchers(level);
        if (watchers.isEmpty()) return;
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length < 1.0E-4) return;
        trace(level, watchers, kind, from, along.scale(1.0 / length), Math.min(stop, length), length);
    }

    public static void detection(ServerLevel level, Ray kind, Vec3 origin, Vec3 dir, double length,
                                 double rx, double ry, double rz, double coneAngle) {
        if (length <= 0.0) return;
        Emitter out = emitter(level, kind.faint, RAY_POINTS);
        if (out == null) return;
        if (coneAngle >= 0.0) {
            sector(out, origin, dir, length, coneAngle);
        } else if (Math.max(rx, Math.max(ry, rz)) > 0.0) {
            beam(out, origin, dir, length, rx, ry, rz);
        } else {
            ring(out, origin.add(dir.scale(length)), dir, MARK_RADIUS);
        }
    }

    public static void mark(ServerLevel level, Ray kind, BlockPos pos, boolean passed) {
        Emitter out = emitter(level, passed ? kind.solid : kind.faint, RAY_POINTS);
        if (out == null) return;
        box(out, Vec3.atCenterOf(pos), BLOCK_HALF, BLOCK_HALF, BLOCK_HALF, RAY_DENSITY);
    }

    public static void mark(ServerLevel level, Ray kind, Entity entity, boolean passed) {
        Emitter out = emitter(level, passed ? kind.solid : kind.faint, RAY_POINTS);
        if (out == null) return;
        AABB bounds = entity.getBoundingBox().inflate(0.02);
        box(out, bounds.getCenter(), bounds.getXsize() * 0.5, bounds.getYsize() * 0.5, bounds.getZsize() * 0.5,
            RAY_DENSITY);
    }

    private static void trace(ServerLevel level, List<ServerPlayer> watchers, Ray kind, Vec3 origin, Vec3 dir,
                              double stop, double end) {
        double cut = Math.max(0.0, stop);
        line(new Emitter(level, watchers, kind.solid, RAY_POINTS), origin, dir, Math.min(LEAD, cut), cut, RAY_STEP);
        if (end - cut < REACH_STEP * 0.5) return;
        line(new Emitter(level, watchers, kind.faint, RAY_POINTS), origin, dir, cut + REACH_STEP, end, REACH_STEP);
    }

    private static void line(Emitter out, Vec3 origin, Vec3 dir, double from, double to, double step) {
        for (double t = from; t < to; t += step) out.at(origin.add(dir.scale(t)));
        out.at(origin.add(dir.scale(to)));
    }

    private static void beam(Emitter out, Vec3 origin, Vec3 dir, double length, double rx, double ry, double rz) {
        Vec3 start = origin.add(dir.scale(Math.min(LEAD, length)));
        Vec3 end = origin.add(dir.scale(length));
        box(out, start, rx, ry, rz, RAY_DENSITY);
        box(out, end, rx, ry, rz, RAY_DENSITY);
        for (int ex = -1; ex <= 1; ex += 2) {
            for (int ey = -1; ey <= 1; ey += 2) {
                for (int ez = -1; ez <= 1; ez += 2) {
                    if (overlapsTwin(dir, ex, ey, ez)) continue;
                    edge(out, start.add(ex * rx, ey * ry, ez * rz), end.add(ex * rx, ey * ry, ez * rz), RAY_DENSITY);
                }
            }
        }
    }

    private static boolean overlapsTwin(Vec3 dir, int ex, int ey, int ez) {
        return (Math.abs(dir.x) > 0.999 && ex * dir.x > 0)
            || (Math.abs(dir.y) > 0.999 && ey * dir.y > 0)
            || (Math.abs(dir.z) > 0.999 && ez * dir.z > 0);
    }

    private static void sector(Emitter out, Vec3 origin, Vec3 dir, double range, double halfAngle) {
        double theta = Math.toRadians(Math.max(0.0, Math.min(180.0, halfAngle)));
        Vec3 side = perpendicular(dir);
        Vec3 other = dir.cross(side);
        Vec3 rimCentre = origin.add(dir.scale(Math.cos(theta) * range));
        double rimRadius = Math.sin(theta) * range;
        int rimSteps = SLANTS * Math.max(3, Math.min(16, (int) Math.ceil(Math.PI * 2 * rimRadius * RAY_DENSITY / SLANTS)));
        int arcSteps = Math.max(6, Math.min(48, (int) Math.ceil(theta * range * RAY_DENSITY)));
        for (int i = 0; i < rimSteps; i++) {
            double a = (Math.PI * 2 * i) / rimSteps;
            Vec3 spoke = side.scale(Math.cos(a)).add(other.scale(Math.sin(a)));
            Vec3 rim = rimCentre.add(spoke.scale(rimRadius));
            out.at(rim);
            if (i % (rimSteps / SLANTS) != 0) continue;
            Vec3 slant = rim.subtract(origin);
            double slantLength = slant.length();
            if (slantLength > LEAD) edge(out, origin.add(slant.scale(LEAD / slantLength)), rim, RAY_DENSITY);
            for (int s = 1; s < arcSteps; s++) {
                double alpha = theta * (1.0 - (double) s / arcSteps);
                out.at(origin.add(dir.scale(Math.cos(alpha) * range)).add(spoke.scale(Math.sin(alpha) * range)));
            }
        }
        out.at(origin.add(dir.scale(range)));
    }

    private static void ring(Emitter out, Vec3 centre, Vec3 normal, double radius) {
        Vec3 side = perpendicular(normal);
        Vec3 other = normal.cross(side);
        int steps = Math.max(8, Math.min(RING_STEPS, (int) (radius * 24.0)));
        for (int i = 0; i < steps; i++) {
            double a = (Math.PI * 2 * i) / steps;
            out.at(centre.add(side.scale(Math.cos(a) * radius)).add(other.scale(Math.sin(a) * radius)));
        }
    }

    private static Vec3 perpendicular(Vec3 dir) {
        Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        return dir.cross(up).normalize();
    }

    private static void box(Emitter out, Vec3 c, double rx, double ry, double rz, double density) {
        for (int ex = -1; ex <= 1; ex += 2) {
            for (int ez = -1; ez <= 1; ez += 2) {
                edge(out, c.add(ex * rx, -ry, ez * rz), c.add(ex * rx, ry, ez * rz), density);
            }
        }
        for (int ey = -1; ey <= 1; ey += 2) {
            for (int ez = -1; ez <= 1; ez += 2) {
                edge(out, c.add(-rx, ey * ry, ez * rz), c.add(rx, ey * ry, ez * rz), density);
            }
            for (int ex = -1; ex <= 1; ex += 2) {
                edge(out, c.add(ex * rx, ey * ry, -rz), c.add(ex * rx, ey * ry, rz), density);
            }
        }
    }

    private static void ellipsoid(Emitter out, Vec3 c, double rx, double ry, double rz) {
        for (int i = 0; i < RING_STEPS; i++) {
            double a = (Math.PI * 2 * i) / RING_STEPS;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            out.at(c.add(cos * rx, sin * ry, 0));
            out.at(c.add(cos * rx, 0, sin * rz));
            out.at(c.add(0, cos * ry, sin * rz));
        }
    }

    private static void star(Emitter out, Vec3 c, double rx, double ry, double rz) {
        Vec3[] axes = {
            c.add(rx, 0, 0), c.add(-rx, 0, 0),
            c.add(0, ry, 0), c.add(0, -ry, 0),
            c.add(0, 0, rz), c.add(0, 0, -rz)
        };
        for (int x = 0; x < 2; x++) {
            for (int y = 2; y < 4; y++) {
                for (int z = 4; z < 6; z++) {
                    edge(out, axes[x], axes[y], SHAPE_DENSITY);
                    edge(out, axes[y], axes[z], SHAPE_DENSITY);
                    edge(out, axes[z], axes[x], SHAPE_DENSITY);
                }
            }
        }
    }

    private static void cone(Emitter out, Vec3 c, double rx, double ry, double rz) {
        Vec3 top = c.add(0, ry, 0);
        for (int i = 0; i < RING_STEPS; i++) {
            double a = (Math.PI * 2 * i) / RING_STEPS;
            Vec3 rim = top.add(Math.cos(a) * rx, 0, Math.sin(a) * rz);
            out.at(rim);
            if (i % 6 == 0) edge(out, c, rim, SHAPE_DENSITY);
        }
    }

    private static void edge(Emitter out, Vec3 from, Vec3 to, double density) {
        double length = from.distanceTo(to);
        int steps = (int) Math.max(2, Math.min(20 * density, length * density));
        for (int i = 0; i <= steps; i++) {
            out.at(from.add(to.subtract(from).scale((double) i / steps)));
        }
    }

    private static @Nullable Emitter emitter(ServerLevel level, DustParticleOptions options, int budget) {
        List<ServerPlayer> watchers = DevMode.watchers(level);
        return watchers.isEmpty() ? null : new Emitter(level, watchers, options, budget);
    }

    private static final class Emitter {
        private final ServerLevel level;
        private final List<ServerPlayer> watchers;
        private final DustParticleOptions options;
        private int budget;

        Emitter(ServerLevel level, List<ServerPlayer> watchers, DustParticleOptions options, int budget) {
            this.level = level;
            this.watchers = watchers;
            this.options = options;
            this.budget = budget;
        }

        void at(Vec3 pos) {
            if (budget-- <= 0) return;
            if (!spend(this.level)) return;
            for (int i = 0; i < watchers.size(); i++) {
                level.sendParticles(watchers.get(i), options, true, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static boolean spend(ServerLevel level) {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            tickBudget = TICK_BUDGET;
        }
        return tickBudget-- > 0;
    }

    private record Drawn(int first, int second, Object config) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Drawn drawn && drawn.first == first && drawn.second == second
                && drawn.config == config;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * first + second) + System.identityHashCode(config);
        }
    }
}
