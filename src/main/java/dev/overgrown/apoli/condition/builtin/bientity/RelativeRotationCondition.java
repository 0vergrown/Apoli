package dev.overgrown.apoli.condition.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RelativeRotationCondition implements ConditionType<BiEntityCtx, RelativeRotationCondition.Cfg> {
    public record Cfg(
        List<Axis> axes,
        RotationType actorRotation,
        RotationType targetRotation,
        Comparison comparison,
        double compareTo
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Axis.LIST_CODEC.optionalFieldOf("axes", List.of(Axis.X, Axis.Y, Axis.Z)).forGetter(Cfg::axes),
            RotationType.CODEC.optionalFieldOf("actor_rotation", RotationType.HEAD).forGetter(Cfg::actorRotation),
            RotationType.CODEC.optionalFieldOf("target_rotation", RotationType.BODY).forGetter(Cfg::targetRotation),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.DOUBLE.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        if (ctx.actor() == null || ctx.target() == null) return false;
        Vec3 actor = reduce(cfg.actorRotation.vector(ctx.actor()), cfg.axes);
        Vec3 target = reduce(cfg.targetRotation.vector(ctx.target()), cfg.axes);
        double actorLen = actor.length();
        double targetLen = target.length();
        if (actorLen == 0 || targetLen == 0) return false;
        double dot = actor.dot(target) / (actorLen * targetLen);
        return cfg.comparison.compare(dot, cfg.compareTo);
    }

    private static Vec3 reduce(Vec3 v, List<Axis> keep) {
        return new Vec3(
            keep.contains(Axis.X) ? v.x : 0,
            keep.contains(Axis.Y) ? v.y : 0,
            keep.contains(Axis.Z) ? v.z : 0
        );
    }

    public enum Axis implements StringRepresentable {
        X("x"), Y("y"), Z("z");

        public static final Codec<Axis> CODEC = StringRepresentable.fromEnum(Axis::values);
        public static final Codec<List<Axis>> LIST_CODEC = CODEC.listOf();

        private final String name;
        Axis(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }
    }

    public enum RotationType implements StringRepresentable {
        HEAD("head"),
        BODY("body");

        public static final Codec<RotationType> CODEC = StringRepresentable.fromEnum(RotationType::values);

        private final String name;
        RotationType(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }

        public Vec3 vector(Entity entity) {
            if (this == HEAD || !(entity instanceof LivingEntity living)) {
                return entity.getViewVector(1.0f);
            }
            float pitchRad = living.getXRot() * Mth.DEG_TO_RAD;
            float yawRad = -living.yBodyRot * Mth.DEG_TO_RAD;
            float cosY = Mth.cos(yawRad), sinY = Mth.sin(yawRad);
            float cosP = Mth.cos(pitchRad), sinP = Mth.sin(pitchRad);
            return new Vec3(sinY * cosP, -sinP, cosY * cosP);
        }
    }
}
