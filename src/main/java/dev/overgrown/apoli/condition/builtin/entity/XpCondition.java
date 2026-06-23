package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;

public final class XpCondition implements ConditionType<EntityCtx, XpCondition.Cfg> {
    public enum Unit implements StringRepresentable {
        LEVELS("levels"),
        POINTS("points");

        public static final Codec<Unit> CODEC = StringRepresentable.fromEnum(Unit::values);
        private final String name;
        Unit(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(Unit unit, Comparison comparison, int compareTo) {}

    private final Unit defaultUnit;

    public XpCondition(Unit defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public XpCondition() {
        this(Unit.LEVELS);
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Unit.CODEC.optionalFieldOf("unit", defaultUnit).forGetter(Cfg::unit),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        int val = cfg.unit == Unit.LEVELS ? player.experienceLevel : player.totalExperience;
        return cfg.comparison.compare(val, cfg.compareTo);
    }
}
