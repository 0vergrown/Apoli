package dev.overgrown.apoli.client.rope;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.overgrown.apoli.rope.RopeConstants.GOAL_ROPE_SEGMENT_LENGTH;

public class VerletRopeState {

    public final UUID owner;
    public final Vec3 anchor;
    public double length;
    public double targetLength;
    public float maxLength;
    public List<RopePoint> points;
    public double segmentLength;
    public ResourceLocation texture;

    public VerletRopeState(UUID owner, Vec3 anchor, double length, float maxLength, ResourceLocation texture) {
        this.owner = owner;
        this.anchor = anchor;
        this.length = length;
        this.targetLength = length;
        this.maxLength = maxLength;
        this.texture = texture;

        int segments = Math.max(2, (int) Math.round(length / GOAL_ROPE_SEGMENT_LENGTH));
        this.segmentLength = length / segments;
        this.points = new ArrayList<>(segments + 1);

        Vec3 playerPos = anchor.subtract(0, length, 0);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Player p = level.getPlayerByUUID(owner);
            if (p != null) {
                playerPos = p.getBoundingBox().getCenter();
            }
        }

        Vec3 jitter = new Vec3(
            (Math.random() - 0.5) * 1e-4,
            0,
            (Math.random() - 0.5) * 1e-4
        );

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 p = anchor.lerp(playerPos, t);
            points.add(new RopePoint(p.add(jitter)));
        }
    }
}
