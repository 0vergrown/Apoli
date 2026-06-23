package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.player.Player;

public final class HungerCondition implements ConditionType<EntityCtx, HungerCondition.Cfg> {
    public enum Kind implements net.minecraft.util.StringRepresentable {
        FOOD("food"),
        SATURATION("saturation");

        public static final Codec<Kind> CODEC = net.minecraft.util.StringRepresentable.fromEnum(Kind::values);
        private final String name;
        Kind(String n) { this.name = n; }
        @Override public String getSerializedName() { return name; }
    }

    public record Cfg(Kind kind, Comparison comparison, float compareTo) {}

    private final Kind defaultKind;

    public HungerCondition(Kind defaultKind) {
        this.defaultKind = defaultKind;
    }

    public HungerCondition() {
        this(Kind.FOOD);
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.optionalFieldOf("kind", defaultKind).forGetter(Cfg::kind),
            Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
            Codec.FLOAT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        float val = cfg.kind == Kind.FOOD
            ? player.getFoodData().getFoodLevel()
            : player.getFoodData().getSaturationLevel();
        return cfg.comparison.compare(val, cfg.compareTo);
    }
}
