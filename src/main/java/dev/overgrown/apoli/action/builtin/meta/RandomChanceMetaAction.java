package dev.overgrown.apoli.action.builtin.meta;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RandomChanceMetaAction<CTX, W> implements ActionType<CTX, RandomChanceMetaAction.Cfg<W>> {
    private final Codec<W> wrapperCodec;
    private final BiConsumer<W, CTX> runner;
    private final Function<CTX, @Nullable LivingEntity> entityGetter;
    private final Function<CTX, @Nullable Level> levelGetter;

    public RandomChanceMetaAction(Codec<W> wrapperCodec, BiConsumer<W, CTX> runner,
                                  Function<CTX, @Nullable LivingEntity> entityGetter,
                                  Function<CTX, @Nullable Level> levelGetter) {
        this.wrapperCodec = wrapperCodec;
        this.runner = runner;
        this.entityGetter = entityGetter;
        this.levelGetter = levelGetter;
    }

    public record Cfg<W>(Expression chance, W action, Optional<W> failAction) {}

    @Override
    public MapCodec<Cfg<W>> codec() {
        return AliasingMapCodec.wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                Expression.FLOAT_OR_EXPR.fieldOf("chance").forGetter(Cfg<W>::chance),
                wrapperCodec.fieldOf("action").forGetter(Cfg<W>::action),
                wrapperCodec.optionalFieldOf("fail_action").forGetter(Cfg<W>::failAction)
            ).apply(i, Cfg::new)),
            Map.of("success_action", "action")
        );
    }

    @Override
    public void run(Cfg<W> cfg, CTX ctx) {
        LivingEntity entity = entityGetter.apply(ctx);
        double chance = entity != null ? cfg.chance.eval(entity) : cfg.chance.eval(levelGetter.apply(ctx));
        if (ThreadLocalRandom.current().nextDouble() < chance) runner.accept(cfg.action, ctx);
        else cfg.failAction.ifPresent(a -> runner.accept(a, ctx));
    }

    @Override
    public boolean acceptsNonLiving() {
        return true;
    }
}
