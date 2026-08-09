package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Vector;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ModifyBlockStuckSpeedHandler {
    private ModifyBlockStuckSpeedHandler() {}

    public static Vec3 modify(Entity entity, BlockState state, Vec3 original) {
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        if (container.powersOfType(ApoliIds.MODIFY_BLOCK_STUCK_SPEED).isEmpty()) return original;

        BlockCtx ctx = new BlockCtx(entity.blockPosition(), state, entity.level());
        Vec3[] result = {original};
        PowerLookup.forEach(entity, ApoliIds.MODIFY_BLOCK_STUCK_SPEED,
            ModifyBlockStuckSpeedPower.Config.class, cfg -> {
                if (cfg.blockCondition().isPresent() && !cfg.blockCondition().get().test(ctx)) return;
                Vector m = cfg.multiplier();
                result[0] = new Vec3(
                    Math.max(result[0].x, m.x()),
                    Math.max(result[0].y, m.y()),
                    Math.max(result[0].z, m.z()));
            });
        return result[0];
    }

    public static boolean fullyFree(Vec3 multiplier) {
        return multiplier.x >= 1.0 && multiplier.y >= 1.0 && multiplier.z >= 1.0;
    }

    public static float modifySpeedFactor(Entity entity, float original) {
        if (original >= 1.0f) return original;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return original;
        if (container.powersOfType(ApoliIds.MODIFY_BLOCK_STUCK_SPEED).isEmpty()) return original;

        BlockPos pos = entity.blockPosition();
        BlockState state = entity.level().getBlockState(pos);
        BlockState below = entity.level().getBlockState(pos.below());
        BlockCtx atFeet = new BlockCtx(pos, state, entity.level());
        BlockCtx underneath = new BlockCtx(pos.below(), below, entity.level());

        float[] result = {original};
        PowerLookup.forEach(entity, ApoliIds.MODIFY_BLOCK_STUCK_SPEED,
            ModifyBlockStuckSpeedPower.Config.class, cfg -> {
                if (cfg.blockCondition().isPresent()
                    && !cfg.blockCondition().get().test(atFeet)
                    && !cfg.blockCondition().get().test(underneath)) return;
                float m = Math.max(cfg.multiplier().x(), Math.max(cfg.multiplier().y(), cfg.multiplier().z()));
                if (m > result[0]) result[0] = m;
            });
        return Math.min(result[0], 1.0f);
    }
}
