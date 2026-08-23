package dev.overgrown.apoli.condition.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

public final class DifficultyCondition implements ConditionType<EntityCtx, DifficultyCondition.Cfg> {

    public enum Level_ implements StringRepresentable {
        PEACEFUL(Difficulty.PEACEFUL),
        EASY(Difficulty.EASY),
        NORMAL(Difficulty.NORMAL),
        HARD(Difficulty.HARD);

        public static final Codec<Level_> CODEC = StringRepresentable.fromEnum(Level_::values);

        private final Difficulty vanilla;

        Level_(Difficulty vanilla) {
            this.vanilla = vanilla;
        }

        public Difficulty vanilla() {
            return vanilla;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Cfg(List<Level_> difficulties) {}

    private static final Codec<List<Level_>> LIST_OR_SINGLE = Codec.either(
        Level_.CODEC, Level_.CODEC.listOf()
    ).xmap(either -> either.map(List::of, list -> list),
        list -> list.size() == 1
            ? com.mojang.datafixers.util.Either.left(list.get(0))
            : com.mojang.datafixers.util.Either.right(list));

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            LIST_OR_SINGLE.fieldOf("difficulty").forGetter(Cfg::difficulties)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        Level level = ctx.level();
        if (level == null) return false;
        Difficulty current = level.getDifficulty();
        for (int i = 0; i < cfg.difficulties.size(); i++) {
            if (cfg.difficulties.get(i).vanilla() == current) return true;
        }
        return false;
    }
}
