package dev.overgrown.apoli.rope;

import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.RopeCreateS2C;
import dev.overgrown.apoli.network.payload.RopeDeleteS2C;
import dev.overgrown.apoli.network.payload.RopeVerletLengthS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.overgrown.apoli.rope.RopeConstants.*;

public final class RopeManager {
    private RopeManager() {}

    private static final Map<UUID, RopeState> ROPES = new HashMap<>();

    public static void attach(ServerPlayer player, Vec3 anchor, float maxLength, ResourceLocation texture) {
        RopeState rope = new RopeState(anchor, player, maxLength, texture);
        ROPES.put(player.getUUID(), rope);

        if (player.isFallFlying()) {
            if (player.isShiftKeyDown()) {
                player.stopFallFlying();
            } else {
                rope.length = Mth.clamp(rope.length - ELYTRA_LENGTH_MOD, MIN_ROPE_LENGTH, maxLength);
            }
        }

        ApoliNetwork.sendRopeCreate(player,
            new RopeCreateS2C(rope.owner, rope.anchor, rope.length, rope.maxLength, rope.texture));
    }

    public static void detach(ServerPlayer player) {
        ROPES.remove(player.getUUID());
        ApoliNetwork.sendRopeDelete(player, new RopeDeleteS2C(player.getUUID()));
    }

    public static RopeState get(UUID uuid) {
        return ROPES.get(uuid);
    }

    public static void clear() {
        ROPES.clear();
    }

    private static void applyLeashConstraint(ServerPlayer player, RopeState rope) {
        Vec3 anchor = rope.anchor;
        Vec3 pos = player.getBoundingBox().getCenter();
        Vec3 delta = pos.subtract(anchor);
        double dist = delta.length();

        if (dist <= rope.length) return;

        if (rope.anchor.y > player.getY()) {
            player.fallDistance = Math.max(0, player.fallDistance - 1.0f);
        }

        Vec3 dir = delta.normalize();
        double excess = dist - rope.length;

        Vec3 vel = player.getDeltaMovement();
        double radial = vel.dot(dir);

        Vec3 radialVel = dir.scale(radial);
        Vec3 tangentialVel = vel.subtract(radialVel);

        if (radial > 0) {
            radialVel = radialVel.scale(RADIAL_DAMPING);
        }

        if (player.isSprinting() && tangentialVel.length() < MAX_SWING_SPEED) {
            tangentialVel = tangentialVel.normalize().scale(tangentialVel.length() * SWING_BOOST);
        }

        player.setDeltaMovement(tangentialVel.add(radialVel));

        double springScale = radial < 0 ? SPRING_SCALING : 1.0;
        Vec3 correction = dir.scale(-excess * LEASH_STIFFNESS * springScale);
        player.setDeltaMovement(player.getDeltaMovement().add(correction));

        player.hurtMarked = true;
    }

    public static void handleLengthChange(ServerPlayer player, double delta) {
        RopeState rope = ROPES.get(player.getUUID());
        if (rope == null) return;

        double dist = player.getBoundingBox().getCenter().subtract(rope.anchor).length();
        if (delta < 0 && dist < 0.95 * rope.length) {
            delta *= SLACK_PULL_RATE_MULT;
        }

        rope.length = Mth.clamp(rope.length + delta, MIN_ROPE_LENGTH, rope.maxLength);
        ApoliNetwork.sendRopeVerletLength(player, new RopeVerletLengthS2C(rope.owner, rope.length));
    }

    public static void applySwingInput(ServerPlayer player, Vec3 inputDir) {
        RopeState rope = ROPES.get(player.getUUID());
        if (rope == null) return;

        Vec3 forward = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        Vec3 localMomentum = forward.scale(inputDir.z).add(right.scale(inputDir.x));

        double swingForce = 0.02;
        player.setDeltaMovement(player.getDeltaMovement().add(localMomentum.scale(swingForce)));
        player.hurtMarked = true;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RopeState rope = ROPES.get(player.getUUID());
            if (rope == null) continue;

            if (player.isFallFlying()) {
                rope.playerFlightTicks += 1;
                if (rope.playerFlightTicks >= ELYTRA_TIME_LIMIT) {
                    detach(player);
                    continue;
                }
            } else {
                rope.playerFlightTicks = 0;
            }

            applyLeashConstraint(player, rope);
        }
    }
}
