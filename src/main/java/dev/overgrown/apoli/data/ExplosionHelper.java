package dev.overgrown.apoli.data;

import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ExplosionHelper {
    private static final float INDESTRUCTIBLE_RESISTANCE = 1_000_000f;

    private ExplosionHelper() {}

    public static void detonate(Level level, @Nullable Entity source, Vec3 pos, float power,
                                boolean createFire, DestructionType destructionType,
                                Optional<BlockCondition> indestructible,
                                Optional<BlockCondition> destructible) {
        if (level.isClientSide()) return;
        DamageSource damageSource = level.damageSources().explosion(source, source);
        ExplosionDamageCalculator calculator = (indestructible.isPresent() || destructible.isPresent())
            ? new ConditionedCalculator(level, indestructible.orElse(null), destructible.orElse(null))
            : null;

        Explosion explosion = new Explosion(
            level, source, damageSource, calculator,
            pos.x, pos.y, pos.z, power, createFire, destructionType.vanilla(),
            ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE
        );
        explosion.explode();
        explosion.finalizeExplosion(true);
    }

    private static final class ConditionedCalculator extends ExplosionDamageCalculator {
        private final Level level;
        private final @Nullable BlockCondition indestructible;
        private final @Nullable BlockCondition destructible;

        ConditionedCalculator(Level level,
                              @Nullable BlockCondition indestructible,
                              @Nullable BlockCondition destructible) {
            this.level = level;
            this.indestructible = indestructible;
            this.destructible = destructible;
        }

        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, net.minecraft.world.level.BlockGetter blocks,
                                                            BlockPos pos, BlockState state, FluidState fluid) {
            BlockCtx ctx = new BlockCtx(pos.immutable(), state, level);
            if (destructible != null && destructible.test(ctx)) {
                return super.getBlockExplosionResistance(explosion, blocks, pos, state, fluid);
            }
            if (indestructible != null && indestructible.test(ctx)) {
                return Optional.of(INDESTRUCTIBLE_RESISTANCE);
            }
            if (destructible != null) {
                return Optional.of(INDESTRUCTIBLE_RESISTANCE);
            }
            return super.getBlockExplosionResistance(explosion, blocks, pos, state, fluid);
        }
    }
}
