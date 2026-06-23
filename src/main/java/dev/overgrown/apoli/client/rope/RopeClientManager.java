package dev.overgrown.apoli.client.rope;

import dev.overgrown.apoli.network.payload.RopeChangeLengthC2S;
import dev.overgrown.apoli.network.payload.RopeSwingC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.overgrown.apoli.rope.RopeConstants.*;

public final class RopeClientManager {
    private RopeClientManager() {}

    private static final Map<UUID, VerletRopeState> ROPES = new HashMap<>();

    public static void attach(UUID owner, Vec3 anchor, double length, float maxLength, ResourceLocation texture) {
        ROPES.put(owner, new VerletRopeState(owner, anchor, length, maxLength, texture));
    }

    public static void detach(UUID owner) {
        ROPES.remove(owner);
    }

    public static VerletRopeState get(UUID uuid) {
        return ROPES.get(uuid);
    }

    public static Collection<VerletRopeState> getAll() {
        return ROPES.values();
    }

    public static void clear() {
        ROPES.clear();
    }

    private static void verletStep(VerletRopeState rope) {
        for (int i = 1; i < rope.points.size(); i++) {
            RopePoint p = rope.points.get(i);
            Vec3 vel = p.pos.subtract(p.prevPos).scale(ROPE_DAMPING);
            p.prevPos = p.pos;
            p.pos = p.pos.add(vel).add(GRAVITY);
        }
    }

    private static void applyRopeConstraints(VerletRopeState rope, Vec3 playerPos) {
        rope.points.get(0).pos = rope.anchor;
        rope.points.get(rope.points.size() - 1).pos = playerPos;

        for (int i = 0; i < rope.points.size() - 1; i++) {
            RopePoint a = rope.points.get(i);
            RopePoint b = rope.points.get(i + 1);

            Vec3 delta = b.pos.subtract(a.pos);
            double dist = delta.length();
            double diff = (dist - rope.segmentLength) / dist;

            Vec3 offset = delta.scale(ROPE_STIFFNESS * diff);
            a.pos = a.pos.add(offset);
            b.pos = b.pos.subtract(offset);
        }

        rope.points.get(0).pos = rope.anchor;
        rope.points.get(rope.points.size() - 1).pos = playerPos;
    }

    private static void changeRopeLength(VerletRopeState rope, Minecraft client) {
        double delta = 0f;
        if (client.options.keyJump.isDown()) delta -= ROPE_LENGTH_CHANGE_STEP;
        if (client.options.keyShift.isDown()) delta += ROPE_LENGTH_CHANGE_STEP;
        if (delta != 0f) PacketDistributor.sendToServer(new RopeChangeLengthC2S(delta));

        if (rope.targetLength != rope.length) {
            rope.length = Mth.lerp(0.33, rope.length, rope.targetLength);

            int desiredSegments = Math.max(2, (int) Math.ceil(rope.length / GOAL_ROPE_SEGMENT_LENGTH));
            desiredSegments = Mth.clamp(
                desiredSegments,
                Math.round(MIN_ROPE_LENGTH * GOAL_ROPE_SEGMENT_LENGTH),
                Math.round(rope.maxLength * GOAL_ROPE_SEGMENT_LENGTH));

            if (rope.points.size() - 1 > desiredSegments) {
                rope.points.remove(1);
            }
            if (rope.points.size() - 1 < desiredSegments) {
                RopePoint last = rope.points.get(0);
                RopePoint prev = rope.points.get(1);
                Vec3 dir = last.pos.subtract(prev.pos).normalize();
                Vec3 newPos = last.pos.add(dir.scale(GOAL_ROPE_SEGMENT_LENGTH));

                RopePoint p = new RopePoint(newPos);
                Vec3 vel = last.pos.subtract(last.prevPos);
                p.prevPos = newPos.subtract(vel);

                rope.points.add(p);
            }

            rope.segmentLength = rope.length / (rope.points.size() - 1);
        }
    }

    private static void swing(Minecraft client) {
        Options opts = client.options;

        double x = 0;
        double z = 0;
        if (opts.keyUp.isDown())    z += 1;
        if (opts.keyDown.isDown())  z -= 1;
        if (opts.keyRight.isDown()) x += 1;
        if (opts.keyLeft.isDown())  x -= 1;
        if (x == 0 && z == 0) return;

        Vec3 dir = new Vec3(x, 0, z);
        PacketDistributor.sendToServer(new RopeSwingC2S(dir.lengthSqr() > 1 ? dir.normalize() : dir));
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) return;

        UUID localUuid = client.player.getUUID();
        VerletRopeState localRope = ROPES.get(localUuid);
        if (localRope != null) {
            changeRopeLength(localRope, client);

            Vec3 clientPos = client.player.getBoundingBox().getCenter();
            double dist = clientPos.subtract(localRope.anchor).length();
            if (localRope.anchor.y > clientPos.y && dist >= localRope.length * 0.98) {
                swing(client);
            }
        }

        for (VerletRopeState rope : ROPES.values()) {
            Player player = level.getPlayerByUUID(rope.owner);
            if (player == null) continue;
            Vec3 playerPos = player.getBoundingBox().getCenter();

            verletStep(rope);
            for (int i = 0; i < 10; i++) {
                applyRopeConstraints(rope, playerPos);
            }
        }
    }
}
