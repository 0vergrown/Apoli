package dev.overgrown.apoli.dev;

import dev.overgrown.apoli.data.Shape;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class DevParticles {

    private static final DustParticleOptions BLUE =
        new DustParticleOptions(new Vector3f(0.25F, 0.55F, 1.0F), 0.75F);
    private static final DustParticleOptions RED =
        new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.2F), 0.75F);

    private static final int RING_STEPS = 48;
    private static final int MAX_POINTS = 600;

    private DevParticles() {}

    public static void outlineShape(ServerLevel level, Vec3 origin, Shape shape, double rx, double ry, double rz) {
        List<ServerPlayer> watchers = DevMode.watchers(level);
        if (watchers.isEmpty()) return;
        Emitter out = new Emitter(level, watchers, BLUE);
        switch (shape) {
            case CUBE -> box(out, origin, rx, ry, rz);
            case SPHERE -> ellipsoid(out, origin, rx, ry, rz);
            case STAR -> star(out, origin, rx, ry, rz);
            case CONE -> cone(out, origin, rx, ry, rz);
        }
    }

    public static void outlineRay(ServerLevel level, Vec3 origin, Vec3 end, double radius, double coneAngleDegrees) {
        List<ServerPlayer> watchers = DevMode.watchers(level);
        if (watchers.isEmpty()) return;
        Emitter out = new Emitter(level, watchers, RED);
        Vec3 along = end.subtract(origin);
        double length = along.length();
        if (length < 1.0E-4) return;
        Vec3 dir = along.scale(1.0 / length);
        for (double t = 0; t <= length; t += 0.5) out.at(origin.add(dir.scale(t)));

        double endRadius = coneAngleDegrees > 0
            ? Math.tan(Math.toRadians(Math.min(coneAngleDegrees, 89.0))) * length
            : radius;
        if (endRadius <= 0.0) return;

        Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up).normalize();
        Vec3 other = dir.cross(side).normalize();
        for (int i = 0; i < RING_STEPS; i++) {
            double a = (Math.PI * 2 * i) / RING_STEPS;
            Vec3 offset = side.scale(Math.cos(a) * endRadius).add(other.scale(Math.sin(a) * endRadius));
            out.at(end.add(offset));
            if (i % 6 == 0) {
                Vec3 from = coneAngleDegrees > 0 ? origin : origin.add(offset);
                for (double t = 0; t <= 1.0; t += 0.1) out.at(from.add(end.add(offset).subtract(from).scale(t)));
            }
        }
    }

    private static void box(Emitter out, Vec3 c, double rx, double ry, double rz) {
        for (int ex = -1; ex <= 1; ex += 2) {
            for (int ez = -1; ez <= 1; ez += 2) {
                edge(out, c.add(ex * rx, -ry, ez * rz), c.add(ex * rx, ry, ez * rz));
            }
        }
        for (int ey = -1; ey <= 1; ey += 2) {
            for (int ez = -1; ez <= 1; ez += 2) {
                edge(out, c.add(-rx, ey * ry, ez * rz), c.add(rx, ey * ry, ez * rz));
            }
            for (int ex = -1; ex <= 1; ex += 2) {
                edge(out, c.add(ex * rx, ey * ry, -rz), c.add(ex * rx, ey * ry, rz));
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
                    edge(out, axes[x], axes[y]);
                    edge(out, axes[y], axes[z]);
                    edge(out, axes[z], axes[x]);
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
            if (i % 6 == 0) edge(out, c, rim);
        }
    }

    private static void edge(Emitter out, Vec3 from, Vec3 to) {
        double length = from.distanceTo(to);
        int steps = (int) Math.max(2, Math.min(40, length * 2));
        for (int i = 0; i <= steps; i++) {
            out.at(from.add(to.subtract(from).scale((double) i / steps)));
        }
    }

    private static final class Emitter {
        private final ServerLevel level;
        private final List<ServerPlayer> watchers;
        private final DustParticleOptions options;
        private int budget = MAX_POINTS;

        Emitter(ServerLevel level, List<ServerPlayer> watchers, DustParticleOptions options) {
            this.level = level;
            this.watchers = watchers;
            this.options = options;
        }

        void at(Vec3 pos) {
            if (budget-- <= 0) return;
            for (int i = 0; i < watchers.size(); i++) {
                level.sendParticles(watchers.get(i), options, true, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}
