package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Shape;
import dev.overgrown.apoli.data.Vector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class DistanceFromCoordinatesCondition implements ConditionType<EntityCtx, DistanceFromCoordinatesCondition.Cfg> {
    public enum Reference implements StringRepresentable {
        WORLD_ORIGIN("world_origin"),
        WORLD_SPAWN("world_spawn");

        public static final Codec<Reference> CODEC = StringRepresentable.fromEnum(Reference::values);
        private final String name;
        Reference(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(
        Reference reference,
        Optional<Vector> offset,
        boolean ignoreX,
        boolean ignoreY,
        boolean ignoreZ,
        Shape shape,
        boolean scaleReferenceToDimension,
        Optional<Boolean> resultOnWrongDimension,
        Optional<Integer> roundToDigit,
        Comparison comparison,
        float compareTo
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Reference.CODEC.optionalFieldOf("reference", Reference.WORLD_ORIGIN).forGetter(Cfg::reference),
            Vector.CODEC.optionalFieldOf("offset").forGetter(Cfg::offset),
            Codec.BOOL.optionalFieldOf("ignore_x", false).forGetter(Cfg::ignoreX),
            Codec.BOOL.optionalFieldOf("ignore_y", false).forGetter(Cfg::ignoreY),
            Codec.BOOL.optionalFieldOf("ignore_z", false).forGetter(Cfg::ignoreZ),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            Codec.BOOL.optionalFieldOf("scale_reference_to_dimension", true).forGetter(Cfg::scaleReferenceToDimension),
            Codec.BOOL.optionalFieldOf("result_on_the_wrong_dimension").forGetter(Cfg::resultOnWrongDimension),
            Codec.INT.optionalFieldOf("round_to_digit").forGetter(Cfg::roundToDigit),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Entity e = ctx.raw();
        Level level = ctx.level();
        double refX, refY, refZ;
        if (cfg.reference == Reference.WORLD_SPAWN) {
            net.minecraft.core.BlockPos spawn = level.getSharedSpawnPos();
            refX = spawn.getX(); refY = spawn.getY(); refZ = spawn.getZ();
            MinecraftServer server = level.getServer();
            if (server != null && cfg.resultOnWrongDimension.isPresent()) {
                ServerLevel overworld = server.overworld();
                if (level != overworld) return cfg.resultOnWrongDimension.get();
            }
        } else {
            refX = 0; refY = 0; refZ = 0;
        }
        if (cfg.offset.isPresent()) {
            refX += cfg.offset.get().x();
            refY += cfg.offset.get().y();
            refZ += cfg.offset.get().z();
        }
        if (cfg.scaleReferenceToDimension) {
            double scale = level.dimensionType().coordinateScale();
            refX /= scale; refZ /= scale;
        }
        double dx = cfg.ignoreX ? 0 : e.getX() - refX;
        double dy = cfg.ignoreY ? 0 : e.getY() - refY;
        double dz = cfg.ignoreZ ? 0 : e.getZ() - refZ;
        double dist = switch (cfg.shape) {
            case CUBE -> Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            case SPHERE, CONE -> Math.sqrt(dx * dx + dy * dy + dz * dz);
            case STAR -> Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        };
        if (cfg.roundToDigit.isPresent()) {
            double scale = Math.pow(10, cfg.roundToDigit.get());
            dist = Math.round(dist * scale) / scale;
        }
        return cfg.comparison.compare((float) dist, cfg.compareTo);
    }
}
