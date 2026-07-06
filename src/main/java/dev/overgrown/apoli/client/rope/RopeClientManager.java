package dev.overgrown.apoli.client.rope;

import dev.overgrown.apoli.network.payload.RopeChangeLengthC2S;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeSwingC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static dev.overgrown.apoli.rope.RopeConstants.*;

public final class RopeClientManager {
    private RopeClientManager() {}

    private static final Map<Integer, VerletRopeState> ROPES = new HashMap<>();

    public static void attach(RopeCreateS2C p, ClientLevel level) {
        ROPES.put(p.id(), new VerletRopeState(p.id(), p.from(), p.to(), p.length(), p.maxLength(),
            p.texture(), p.owner(), p.controllable(), level));
    }

    public static void detach(int id) {
        ROPES.remove(id);
    }

    public static VerletRopeState get(int id) {
        return ROPES.get(id);
    }

    public static Collection<VerletRopeState> getAll() {
        return ROPES.values();
    }

    public static void clear() {
        ROPES.clear();
    }

    private static void verletStep(VerletRopeState rope) {
        int last = rope.points.size() - 1;
        RopePoint first = rope.points.get(0);
        first.prevPos = first.pos;
        RopePoint end = rope.points.get(last);
        end.prevPos = end.pos;
        for (int i = 1; i < last; i++) {
            RopePoint p = rope.points.get(i);
            Vec3 vel = p.pos.subtract(p.prevPos).scale(ROPE_DAMPING);
            p.prevPos = p.pos;
            p.pos = p.pos.add(vel).add(GRAVITY);
        }
    }

    private static void applyConstraints(VerletRopeState rope, Vec3 fromPos, Vec3 toPos) {
        int last = rope.points.size() - 1;
        rope.points.get(0).pos = fromPos;
        rope.points.get(last).pos = toPos;

        for (int i = 0; i < last; i++) {
            RopePoint a = rope.points.get(i);
            RopePoint b = rope.points.get(i + 1);
            Vec3 delta = b.pos.subtract(a.pos);
            double dist = delta.length();
            if (dist < 1e-6) continue;
            double diff = (dist - rope.segmentLength) / dist;
            Vec3 offset = delta.scale(ROPE_STIFFNESS * diff);
            a.pos = a.pos.add(offset);
            b.pos = b.pos.subtract(offset);
        }

        rope.points.get(0).pos = fromPos;
        rope.points.get(last).pos = toPos;
    }

    private static void resegment(VerletRopeState rope) {
        if (rope.targetLength == rope.length) return;
        rope.length = Mth.lerp(0.33, rope.length, rope.targetLength);

        int desiredSegments = Math.max(2, (int) Math.ceil(rope.length / GOAL_ROPE_SEGMENT_LENGTH));
        desiredSegments = Math.min(desiredSegments, (int) Math.ceil(rope.maxLength / GOAL_ROPE_SEGMENT_LENGTH));

        if (rope.points.size() - 1 > desiredSegments && rope.points.size() > 2) {
            rope.points.remove(1);
        }
        if (rope.points.size() - 1 < desiredSegments) {
            RopePoint anchor = rope.points.get(0);
            RopePoint next = rope.points.get(1);
            RopePoint p = new RopePoint(anchor.pos.lerp(next.pos, 0.5));
            p.prevPos = anchor.prevPos.lerp(next.prevPos, 0.5);
            rope.points.add(1, p);
        }
        rope.segmentLength = rope.length / (rope.points.size() - 1);
    }

    private static void driveLocalRope(VerletRopeState rope, Minecraft client, ClientLevel level) {
        Options opts = client.options;

        double delta = 0;
        if (opts.keyJump.isDown()) delta -= ROPE_LENGTH_CHANGE_STEP;
        if (opts.keyShift.isDown()) delta += ROPE_LENGTH_CHANGE_STEP;
        if (delta != 0) ClientPlayNetworking.send(new RopeChangeLengthC2S(rope.id, delta));

        Vec3 anchor = rope.anchorAwayFrom(client.player.getId(), level);
        Vec3 playerPos = client.player.getBoundingBox().getCenter();
        if (anchor == null || anchor.y <= playerPos.y || playerPos.distanceTo(anchor) < rope.length * 0.98) return;

        double x = 0, z = 0;
        if (opts.keyUp.isDown()) z += 1;
        if (opts.keyDown.isDown()) z -= 1;
        if (opts.keyRight.isDown()) x += 1;
        if (opts.keyLeft.isDown()) x -= 1;
        if (x == 0 && z == 0) return;
        Vec3 dir = new Vec3(x, 0, z);
        ClientPlayNetworking.send(new RopeSwingC2S(rope.id, dir.lengthSqr() > 1 ? dir.normalize() : dir));
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) return;

        for (VerletRopeState rope : ROPES.values()) {
            if (rope.drivenBy(client.player.getUUID())) driveLocalRope(rope, client, level);

            resegment(rope);
            Vec3 fromPos = rope.from.position(level);
            Vec3 toPos = rope.to.position(level);
            if (fromPos == null || toPos == null) continue;

            verletStep(rope);
            for (int i = 0; i < 10; i++) applyConstraints(rope, fromPos, toPos);
        }
    }
}
