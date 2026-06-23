package dev.overgrown.apoli.mixin.flag;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PhasingMixin {
    private static final boolean DEBUG = Boolean.getBoolean("apoli.debug.phasing");

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"), cancellable = true)
    private void apoli$phaseThrough(BlockGetter getter, BlockPos pos, CollisionContext ctx,
                                    CallbackInfoReturnable<VoxelShape> cir) {
        VoxelShape original = cir.getReturnValue();
        if (original.isEmpty()) return;
        if (!(ctx instanceof EntityCollisionContext esc)) return;
        Entity entity = esc.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        boolean isPlayer = entity instanceof Player;
        if (!PowerLookup.hasActive(living, Apoli.id("phasing"))) {
            if (DEBUG && isPlayer) Apoli.LOGGER.info("[phasing] {} hasActive=false — phasing not granted or its top-level condition (power_active:phantomize) failed",
                entity.level().isClientSide() ? "CLIENT" : "SERVER");
            return;
        }

        BlockState state = (BlockState) (Object) this;
        Level level = living.level();
        boolean standingOnTop = isStandingOnTop(living, original, pos);
        boolean[] allow = new boolean[]{false};
        PowerLookup.forEach(living, Apoli.id("phasing"), PhasingPower.Config.class, cfg -> {
            if (allow[0]) return;
            boolean allowsBlock = PhasingPower.allowsPhasing(cfg, level, pos, state);
            boolean blockedByPhaseDown = standingOnTop && !phaseDownAllowed(cfg, living, level);
            if (DEBUG && isPlayer) Apoli.LOGGER.info("[phasing] {} allows_block={} standing_on_top={} blocked_by_phasedown={}",
                entity.level().isClientSide() ? "CLIENT" : "SERVER", allowsBlock, standingOnTop, blockedByPhaseDown);
            if (!allowsBlock) return;
            if (blockedByPhaseDown) return;
            allow[0] = true;
        });
        if (allow[0]) cir.setReturnValue(Shapes.empty());
    }

    private static boolean isStandingOnTop(LivingEntity entity, VoxelShape shape, BlockPos pos) {
        double margin = entity.onGround() ? 8.05 / 16.0 : 0.0015;
        return entity.getY() > pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) - margin;
    }

    private static boolean phaseDownAllowed(PhasingPower.Config cfg, LivingEntity entity, Level level) {
        if (cfg.phaseDownCondition().isPresent()) {
            return cfg.phaseDownCondition().get().test(new EntityCtx(entity, level));
        }
        return entity.isShiftKeyDown();
    }
}
