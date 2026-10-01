package dev.overgrown.apoli.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class StagedLanding {
    private static final double GROUND_PROBE = 1.0E-3;

    @Nullable
    private static Entity staged;

    private StagedLanding() {}

    public static boolean isStaged(Entity entity) {
        return staged != null && staged == entity;
    }

    @Nullable
    static Entity begin(Entity entity) {
        Entity previous = staged;
        staged = entity;
        return previous;
    }

    static void end(@Nullable Entity previous) {
        staged = previous;
    }

    public static double fluidHeight(Entity entity, TagKey<Fluid> fluid) {
        if (entity.touchingUnloadedChunk()) return 0.0;
        AABB box = entity.getBoundingBox().deflate(0.001);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);
        Level level = entity.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double height = 0.0;
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    FluidState state = level.getFluidState(pos.set(x, y, z));
                    if (!state.is(fluid)) continue;
                    double surface = y + state.getHeight(level, pos);
                    if (surface >= box.minY) height = Math.max(height, surface - box.minY);
                }
            }
        }
        return height;
    }

    public static boolean eyeInFluid(Entity entity, TagKey<Fluid> fluid) {
        double eyeY = entity.getEyeY() - 0.11111111F;
        BlockPos pos = BlockPos.containing(entity.getX(), eyeY, entity.getZ());
        FluidState state = entity.level().getFluidState(pos);
        return state.is(fluid) && pos.getY() + state.getHeight(entity.level(), pos) > eyeY;
    }

    public static boolean onGround(Entity entity) {
        AABB box = entity.getBoundingBox();
        AABB below = new AABB(box.minX, box.minY - GROUND_PROBE, box.minZ, box.maxX, box.minY, box.maxZ);
        return !entity.level().noCollision(entity, below);
    }
}
