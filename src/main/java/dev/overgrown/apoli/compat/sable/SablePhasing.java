package dev.overgrown.apoli.compat.sable;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SablePhasing {

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private SablePhasing() {}

    public static void begin(Entity entity) {
        Scope scope = SCOPE.get();
        scope.push(entity instanceof LivingEntity living
            && PowerLookup.hasActive(living, ApoliIds.PHASING) ? living : null);
    }

    public static void end() {
        SCOPE.get().pop();
    }

    public static VoxelShape filter(VoxelShape shape, BlockGetter getter, BlockPos pos, BlockState state) {
        if (shape.isEmpty()) return shape;
        LivingEntity living = SCOPE.get().current();
        if (living == null) return shape;
        Level level = getter instanceof Level fromGetter ? fromGetter : living.level();
        BlockPos immutable = pos.immutable();
        boolean[] allow = new boolean[]{false};
        PowerLookup.forEach(living, ApoliIds.PHASING, PhasingPower.Config.class, cfg -> {
            if (allow[0]) return;
            if (PhasingPower.allowsPhasing(cfg, level, immutable, state)) allow[0] = true;
        });
        return allow[0] ? Shapes.empty() : shape;
    }

    private static final class Scope {
        private LivingEntity[] stack = new LivingEntity[4];
        private int depth;

        void push(LivingEntity entity) {
            if (depth >= 8) depth = 0;
            if (depth == stack.length) {
                LivingEntity[] grown = new LivingEntity[depth * 2];
                System.arraycopy(stack, 0, grown, 0, depth);
                stack = grown;
            }
            stack[depth++] = entity;
        }

        void pop() {
            if (depth == 0) return;
            stack[--depth] = null;
        }

        LivingEntity current() {
            return depth == 0 ? null : stack[depth - 1];
        }
    }
}
