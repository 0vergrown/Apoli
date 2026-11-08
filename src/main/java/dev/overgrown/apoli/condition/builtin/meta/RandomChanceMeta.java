package dev.overgrown.apoli.condition.builtin.meta;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class RandomChanceMeta<CTX> implements ConditionType<CTX, RandomChanceMeta.Cfg> {
    public record Cfg(Expression chance) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.FLOAT_OR_EXPR.fieldOf("chance").forGetter(Cfg::chance)
    ).apply(i, Cfg::new));

    private final Function<CTX, @Nullable Entity> entityGetter;
    private final Function<CTX, @Nullable Level> levelGetter;

    public RandomChanceMeta(Function<CTX, @Nullable Entity> entityGetter, Function<CTX, @Nullable Level> levelGetter) {
        this.entityGetter = entityGetter;
        this.levelGetter = levelGetter;
    }

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public boolean test(Cfg cfg, CTX ctx) {
        Expression chance = cfg.chance;
        double odds;
        if (chance.constantValue().isPresent()) {
            odds = chance.constantValue().getAsDouble();
        } else {
            Entity entity = entityGetter.apply(ctx);
            odds = entity != null ? chance.eval(entity) : chance.eval(levelGetter.apply(ctx));
        }
        return ThreadLocalRandom.current().nextDouble() < odds;
    }
}
