package dev.overgrown.apoli.data;

import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.expr.ExprPeer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
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

    public static void detonate(Level level, @Nullable Entity self, boolean damageSelf, @Nullable Entity attacker,
                                Vec3 pos, float power, boolean createFire, DestructionType destructionType,
                                Optional<BlockCondition> indestructible,
                                Optional<BlockCondition> destructible,
                                boolean damageTargets,
                                Optional<BiEntityCondition> targetCondition) {
        if (level.isClientSide()) return;
        DamageSource damageSource = damageSource(level, self, attacker != null ? attacker : self);
        ExplosionDamageCalculator calculator =
            (indestructible.isPresent() || destructible.isPresent() || !damageTargets || targetCondition.isPresent())
                ? new ConditionedCalculator(level, indestructible.orElse(null), destructible.orElse(null),
                    damageTargets, self, targetCondition.orElse(null))
                : null;

        Explosion explosion = new Explosion(
            level, damageSelf ? null : self, damageSource, calculator,
            pos.x, pos.y, pos.z, power, createFire, destructionType.vanilla(),
            ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE
        );
        explosion.explode();
        explosion.finalizeExplosion(true);
    }

    public static @Nullable Entity attackerFor(@Nullable Entity entity) {
        if (entity == null) return null;
        Entity actor = ExprPeer.actor();
        Entity credited = actor != null && actor != entity && ExprPeer.target() == entity ? actor : entity;
        if (credited instanceof TraceableEntity traceable) {
            Entity owner = traceable.getOwner();
            if (owner != null) return owner;
        }
        return credited;
    }

    private static DamageSource damageSource(Level level, @Nullable Entity direct, @Nullable Entity causing) {
        if (causing == null) return level.damageSources().explosion(direct, null);
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(DamageTypes.PLAYER_EXPLOSION), direct, causing);
    }

    private static final class ConditionedCalculator extends ExplosionDamageCalculator {
        private final Level level;
        private final @Nullable BlockCondition indestructible;
        private final @Nullable BlockCondition destructible;
        private final boolean damageTargets;
        private final @Nullable Entity self;
        private final @Nullable BiEntityCondition targetCondition;

        ConditionedCalculator(Level level,
                              @Nullable BlockCondition indestructible,
                              @Nullable BlockCondition destructible,
                              boolean damageTargets,
                              @Nullable Entity self,
                              @Nullable BiEntityCondition targetCondition) {
            this.level = level;
            this.indestructible = indestructible;
            this.destructible = destructible;
            this.damageTargets = damageTargets;
            this.self = self;
            this.targetCondition = targetCondition;
        }

        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            if (!damageTargets && entity != self) return false;
            if (targetCondition != null && !targetCondition.test(BiEntityCtx.of(self, entity, level))) return false;
            return super.shouldDamageEntity(explosion, entity);
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
