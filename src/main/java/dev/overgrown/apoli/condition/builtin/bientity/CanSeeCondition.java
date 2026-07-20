package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.FluidHandling;
import dev.overgrown.apoli.data.ShapeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CanSeeCondition implements ConditionType<BiEntityCtx, CanSeeCondition.Cfg> {
    public record Cfg(ShapeType shapeType, FluidHandling fluidHandling) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ShapeType.CODEC.optionalFieldOf("shape_type", ShapeType.VISUAL).forGetter(Cfg::shapeType),
            FluidHandling.CODEC.optionalFieldOf("fluid_handling", FluidHandling.NONE).forGetter(Cfg::fluidHandling)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        Vec3 from = ctx.actor().getEyePosition();
        Vec3 to = ctx.target().getEyePosition();
        ClipContext.Block block = switch (cfg.shapeType) {
            case COLLIDER -> ClipContext.Block.COLLIDER;
            case OUTLINE  -> ClipContext.Block.OUTLINE;
            case VISUAL   -> ClipContext.Block.VISUAL;
        };
        ClipContext.Fluid fluid = switch (cfg.fluidHandling) {
            case ANY         -> ClipContext.Fluid.ANY;
            case NONE        -> ClipContext.Fluid.NONE;
            case SOURCE_ONLY -> ClipContext.Fluid.SOURCE_ONLY;
        };
        HitResult hit = ctx.level().clip(new ClipContext(from, to, block, fluid, ctx.actor()));
        return hit.getType() == HitResult.Type.MISS;
    }
}
