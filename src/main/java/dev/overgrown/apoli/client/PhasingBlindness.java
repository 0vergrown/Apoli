package dev.overgrown.apoli.client;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.PhasingPower;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PhasingBlindness {

    private PhasingBlindness() {}

    public static float viewDistance(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) return -1.0F;
        PowerContainer container = PowerContainer.of(living);
        if (container == null || container.isEmpty()) return -1.0F;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.PHASING);
        if (powers.isEmpty()) return -1.0F;
        EntityCtx ctx = null;
        float best = -1.0F;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof PhasingPower.Config cfg)) continue;
            if (cfg.renderType() != PhasingPower.RenderType.BLINDNESS) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(living, living.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            float view = cfg.viewDistance();
            if (best < 0.0F || view < best) best = view;
        }
        return best;
    }

    public static boolean inWall(@Nullable Entity entity) {
        if (entity == null) return false;
        Level level = entity.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double width = entity.getBbWidth() * 0.8;
        for (int corner = 0; corner < 8; corner++) {
            double x = entity.getX() + ((corner & 1) - 0.5) * width;
            double y = entity.getEyeY() + (((corner >> 1) & 1) - 0.5) * 0.1;
            double z = entity.getZ() + (((corner >> 2) & 1) - 0.5) * width;
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.getRenderShape() != RenderShape.INVISIBLE && state.isViewBlocking(level, pos)) return true;
        }
        return false;
    }
}
