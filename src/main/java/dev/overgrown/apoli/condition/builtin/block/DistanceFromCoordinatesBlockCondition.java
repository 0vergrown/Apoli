package dev.overgrown.apoli.condition.builtin.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;

public final class DistanceFromCoordinatesBlockCondition
    implements ConditionType<BlockCtx, DistanceFromCoordinatesBlockCondition.Cfg> {

    public record Cfg(
        String reference,
        Optional<Vector> offset,
        boolean ignoreX,
        boolean ignoreY,
        boolean ignoreZ,
        String shape,
        boolean scaleReferenceToDimension,
        Optional<Boolean> resultOnWrongDimension,
        Optional<Integer> roundToDigit,
        Comparison comparison,
        float compareTo
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("reference", "world_origin").forGetter(Cfg::reference),
            Vector.CODEC.optionalFieldOf("offset").forGetter(Cfg::offset),
            Codec.BOOL.optionalFieldOf("ignore_x", false).forGetter(Cfg::ignoreX),
            Codec.BOOL.optionalFieldOf("ignore_y", false).forGetter(Cfg::ignoreY),
            Codec.BOOL.optionalFieldOf("ignore_z", false).forGetter(Cfg::ignoreZ),
            Codec.STRING.optionalFieldOf("shape", "cube").forGetter(Cfg::shape),
            Codec.BOOL.optionalFieldOf("scale_reference_to_dimension", true).forGetter(Cfg::scaleReferenceToDimension),
            Codec.BOOL.optionalFieldOf("result_on_the_wrong_dimension").forGetter(Cfg::resultOnWrongDimension),
            Codec.INT.optionalFieldOf("round_to_digit").forGetter(Cfg::roundToDigit),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BlockCtx ctx) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return false;

        double refX = 0, refY = 0, refZ = 0;
        ServerLevel referenceLevel = serverLevel;
        if ("world_spawn".equals(cfg.reference)) {
            referenceLevel = serverLevel.getServer().overworld();
            BlockPos spawn = referenceLevel.getSharedSpawnPos();
            refX = spawn.getX();
            refY = spawn.getY();
            refZ = spawn.getZ();
        }
        if (cfg.offset.isPresent()) {
            Vector v = cfg.offset.get();
            refX += v.x();
            refY += v.y();
            refZ += v.z();
        }
        if (cfg.scaleReferenceToDimension) {
            DimensionType from = referenceLevel.dimensionType();
            DimensionType to = serverLevel.dimensionType();
            double scale = from.coordinateScale() / to.coordinateScale();
            refX *= scale;
            refZ *= scale;
        } else if (!serverLevel.dimension().equals(referenceLevel.dimension())
                   && cfg.resultOnWrongDimension.isPresent()) {
            return cfg.resultOnWrongDimension.get();
        }

        double dx = cfg.ignoreX ? 0 : (ctx.pos().getX() - refX);
        double dy = cfg.ignoreY ? 0 : (ctx.pos().getY() - refY);
        double dz = cfg.ignoreZ ? 0 : (ctx.pos().getZ() - refZ);

        double distance = switch (cfg.shape) {
            case "star" -> Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            case "cube" -> Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            default -> Math.sqrt(dx * dx + dy * dy + dz * dz);
        };

        if (cfg.roundToDigit.isPresent()) {
            double factor = Math.pow(10, cfg.roundToDigit.get());
            distance = Math.round(distance * factor) / factor;
        }

        return cfg.comparison.compare(distance, cfg.compareTo);
    }
}
