package dev.overgrown.apoli.client.rope;

import dev.overgrown.apoli.rope.RopeAnchor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.overgrown.apoli.rope.RopeConstants.GOAL_ROPE_SEGMENT_LENGTH;

public class VerletRopeState {

    public final int id;
    public final RopeAnchor from;
    public final RopeAnchor to;
    public final @Nullable UUID owner;
    public final boolean controllable;
    public double length;
    public double targetLength;
    public float maxLength;
    public List<RopePoint> points;
    public double segmentLength;
    public ResourceLocation texture;

    public VerletRopeState(int id, RopeAnchor from, RopeAnchor to, double length, float maxLength,
                           ResourceLocation texture, @Nullable UUID owner, boolean controllable, Level level) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.owner = owner;
        this.controllable = controllable;
        this.length = length;
        this.targetLength = length;
        this.maxLength = maxLength;
        this.texture = texture;

        int segments = Math.max(2, (int) Math.round(length / GOAL_ROPE_SEGMENT_LENGTH));
        this.segmentLength = length / segments;
        this.points = new ArrayList<>(segments + 1);

        Vec3 a = from.position(level);
        Vec3 b = to.position(level);
        if (a == null) a = (b != null ? b.add(0, length, 0) : Vec3.ZERO);
        if (b == null) b = a.subtract(0, length, 0);

        Vec3 jitter = new Vec3((Math.random() - 0.5) * 1e-4, 0, (Math.random() - 0.5) * 1e-4);
        for (int i = 0; i <= segments; i++) {
            points.add(new RopePoint(a.lerp(b, i / (double) segments).add(jitter)));
        }
    }

    public @Nullable Vec3 anchorAwayFrom(int localEntityId, Level level) {
        if (from instanceof RopeAnchor.OfEntity e && e.networkId() == localEntityId) return to.position(level);
        return from.position(level);
    }

    public boolean drivenBy(UUID localUuid) {
        return controllable && localUuid.equals(owner);
    }
}
