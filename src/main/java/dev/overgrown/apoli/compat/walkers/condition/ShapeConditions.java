package dev.overgrown.apoli.compat.walkers.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.compat.walkers.WalkersBridge;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.Entity;

public final class ShapeConditions {
    private ShapeConditions() {}

    public static final class CanUseShapeAbility implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return WalkersBridge.canUseAbility(ctx.raw());
        }
    }

    public static final class HasShapeAbility implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return WalkersBridge.hasShapeAbility(ctx.raw());
        }
    }

    public static final class ShapeAbilityCooldown implements ConditionType<EntityCtx, ShapeAbilityCooldown.Cfg> {
        public record Cfg(Comparison comparison, int compareTo) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
                Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
            ).apply(i, Cfg::new));
        }

        @Override
        public boolean test(Cfg cfg, EntityCtx ctx) {
            Entity entity = ctx.raw();
            if (entity == null) return false;
            return cfg.comparison.compare(WalkersBridge.abilityCooldown(entity), cfg.compareTo);
        }
    }

    public static final class Shape implements ConditionType<EntityCtx, Shape.Cfg> {
        public record Cfg(BiEntityCondition bientityCondition) {}

        @Override
        public MapCodec<Cfg> codec() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                BiEntityCondition.CODEC.fieldOf("bientity_condition").forGetter(Cfg::bientityCondition)
            ).apply(i, Cfg::new));
        }

        @Override
        public boolean test(Cfg cfg, EntityCtx ctx) {
            Entity entity = ctx.raw();
            if (entity == null) return false;
            return cfg.bientityCondition.test(new BiEntityCtx(entity, WalkersBridge.shapeOrSelf(entity), ctx.level()));
        }
    }
}
