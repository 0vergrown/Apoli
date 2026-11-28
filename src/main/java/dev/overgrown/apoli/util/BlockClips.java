package dev.overgrown.apoli.util;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlockClips {

    private static final int[] SELECTION_BYPASS = new int[2];

    private BlockClips() {}

    public static ClipContext ignoringEntity(Vec3 from, Vec3 to, ClipContext.Block block, ClipContext.Fluid fluid,
                                             Entity entity) {
        return new ClipContext(from, to, block, fluid, entity) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter getter, BlockPos pos) {
                return block.get(state, getter, pos, CollisionContext.empty());
            }
        };
    }

    public static BlockHitResult clipBypassingSelectionLock(Level level, ClipContext context) {
        int side = level.isClientSide() ? 1 : 0;
        SELECTION_BYPASS[side]++;
        try {
            return level.clip(context);
        } finally {
            SELECTION_BYPASS[side]--;
        }
    }

    public static boolean bypassesSelectionLock(Level level) {
        return SELECTION_BYPASS[level.isClientSide() ? 1 : 0] > 0;
    }
}
