package dev.overgrown.apoli.client.rope;

import net.minecraft.world.phys.Vec3;

public class RopePoint {
    public Vec3 pos;
    public Vec3 prevPos;

    public RopePoint(Vec3 pos) {
        this.pos = pos;
        this.prevPos = pos;
    }
}
