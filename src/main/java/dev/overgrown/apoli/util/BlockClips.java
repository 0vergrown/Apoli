package dev.overgrown.apoli.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BlockClips {

    private BlockClips() {}

    public static ClipContext ignoringEntity(Vec3 from, Vec3 to, ClipContext.Block block, ClipContext.Fluid fluid,
                                             Entity entity) {
        return new ClipContext(from, to, block, fluid, CollisionContext.empty());
    }
}
