package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ActionOverTimePower extends PowerType<ActionOverTimePower.Config> {

    public static final ResourceLocation CANONICAL = dev.overgrown.apoli.Apoli.id("action_over_time");

    public record Step(
        Expression interval,
        int fixedInterval,
        Expression onsetDelay,
        EntityAction entityAction,
        Optional<EntityCondition> condition
    ) {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("interval", Expression.INT_OR_EXPR, Expression.constant(20)).forGetter(Step::interval),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("onset_delay", Expression.INT_OR_EXPR, Expression.constant(0)).forGetter(Step::onsetDelay),
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Step::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Step::condition)
        ).apply(instance, Step::build));

        private static Step build(Expression interval, Expression onsetDelay, EntityAction entityAction,
                                  Optional<EntityCondition> condition) {
            return new Step(interval, fixedTicks(interval), onsetDelay, entityAction, condition);
        }
    }

    public record Config(
        Expression interval,
        int fixedInterval,
        Expression onsetDelay,
        Optional<EntityAction> entityAction,
        Optional<EntityAction> risingAction,
        Optional<EntityAction> fallingAction,
        List<Step> steps,
        int stride,
        boolean scheduled
    ) {}

    private static final int TICK_MASK = 0x3FFFFFFF;

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("interval", Expression.INT_OR_EXPR, Expression.constant(20)).forGetter(Config::interval),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("onset_delay", Expression.INT_OR_EXPR, Expression.constant(0)).forGetter(Config::onsetDelay),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("rising_action", EntityAction.CODEC).forGetter(Config::risingAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("falling_action", EntityAction.CODEC).forGetter(Config::fallingAction),
            Step.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Config::steps)
        ).apply(instance, ActionOverTimePower::build));
    }

    private static int fixedTicks(Expression expression) {
        java.util.OptionalDouble constant = expression.constantValue();
        if (constant.isEmpty()) return 0;
        long ticks = Math.round(constant.getAsDouble());
        return ticks > 0 ? (int) Math.min(ticks, Integer.MAX_VALUE) : 1;
    }

    private static Config build(Expression interval, Expression onsetDelay, Optional<EntityAction> entityAction,
                                Optional<EntityAction> risingAction, Optional<EntityAction> fallingAction,
                                List<Step> steps) {
        int fixed = fixedTicks(interval);
        boolean scheduled = fixed <= 0;
        int stride = scheduled ? 1 : fixed;
        for (int i = 0; i < steps.size(); i++) {
            int stepFixed = steps.get(i).fixedInterval();
            if (stepFixed <= 0) {
                scheduled = true;
                stride = 1;
            } else {
                stride = gcd(stride, stepFixed);
            }
        }
        return new Config(interval, fixed, onsetDelay, entityAction, risingAction, fallingAction,
            List.copyOf(steps), Math.max(1, stride), scheduled);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return a;
    }

    @Override
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();

        int stride = cfg.stride;
        int tick = owner.tickCount;
        int phase = Math.floorMod(powerId.hashCode() + owner.getId(), stride);
        if (Math.floorMod(tick - phase, stride) != 0) return;

        if (!(owner.level() instanceof ServerLevel level)) return;

        List<Step> steps = cfg.steps;
        boolean wasActive = holder.getAuxInt(powerId).orElse(0) != 0;
        int[] schedule = cfg.scheduled ? schedule(holder, powerId, steps.size() + 1) : null;
        boolean masterTick = cfg.fixedInterval <= 0 || Math.floorMod(tick - phase, cfg.fixedInterval) == 0;

        if (!masterTick) {
            if (!wasActive || !anyStepDue(steps, tick, phase, schedule)) return;
            runSteps(steps, EntityCtx.of(owner, level), holder, powerId, owner, level.getGameTime(), tick, phase, schedule);
            return;
        }

        EntityCtx ctx = EntityCtx.of(owner, level);
        boolean active = conditionHolds(ctx, powerId);

        if (active) {
            if (!wasActive) {
                cfg.risingAction.ifPresent(a -> a.run(ctx));
                markActivated(holder, powerId, level.getGameTime());
                if (schedule != null) java.util.Arrays.fill(schedule, tick);
            }
            long gameTime = level.getGameTime();
            if (schedule == null || cfg.fixedInterval > 0) {
                if (onsetElapsed(holder, powerId, cfg.onsetDelay, owner, gameTime)) {
                    cfg.entityAction.ifPresent(a -> a.run(ctx));
                }
            } else if (tick - schedule[0] >= 0) {
                if (onsetElapsed(holder, powerId, cfg.onsetDelay, owner, gameTime)) {
                    cfg.entityAction.ifPresent(a -> a.run(ctx));
                }
                schedule[0] = tick + evalTicks(cfg.interval, owner);
            }
            runSteps(steps, ctx, holder, powerId, owner, gameTime, tick, phase, schedule);
        } else if (wasActive) {
            cfg.fallingAction.ifPresent(a -> a.run(ctx));
            clearActivated(holder, powerId);
        }
    }

    private static int @Nullable [] schedule(PowerContainer holder, ResourceLocation powerId, int length) {
        return holder instanceof PowerContainerImpl impl ? impl.scratchInts(powerId, length) : null;
    }

    private static int evalTicks(Expression expression, Entity owner) {
        int ticks = expression.evalInt(owner);
        return ticks > 0 ? ticks : 1;
    }

    private static boolean stepDue(Step step, int slot, int tick, int phase, int @Nullable [] schedule) {
        int fixed = step.fixedInterval();
        if (fixed > 0) return Math.floorMod(tick - phase, fixed) == 0;
        return schedule != null && tick - schedule[slot] >= 0;
    }

    private static boolean anyStepDue(List<Step> steps, int tick, int phase, int @Nullable [] schedule) {
        for (int i = 0; i < steps.size(); i++) {
            if (stepDue(steps.get(i), i + 1, tick, phase, schedule)) return true;
        }
        return false;
    }

    private static void runSteps(List<Step> steps, EntityCtx ctx, PowerContainer holder, ResourceLocation powerId,
                                 Entity owner, long gameTime, int tick, int phase, int @Nullable [] schedule) {
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (!stepDue(step, i + 1, tick, phase, schedule)) continue;
            boolean dynamic = step.fixedInterval() <= 0 && schedule != null;
            if (!onsetElapsed(holder, powerId, step.onsetDelay(), owner, gameTime)) continue;
            if (dynamic) schedule[i + 1] = tick + evalTicks(step.interval(), owner);
            if (step.condition().isPresent() && !step.condition().get().test(ctx)) continue;
            step.entityAction().run(ctx);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        if (holder.getAuxInt(powerId).orElse(0) != 0
            && holder.rawOwner().level() instanceof ServerLevel level) {
            cfg.fallingAction.ifPresent(a -> a.run(EntityCtx.of(holder.rawOwner(), level)));
        }
        clearActivated(holder, powerId);
    }

    public static void resetEdges(Entity entity) {
        PowerContainer container = PowerContainer.of(entity);
        if (!(container instanceof PowerContainerImpl impl) || impl.isEmpty()) return;
        java.util.List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        for (int i = 0; i < powers.size(); i++) {
            impl.removeAux(powers.get(i));
        }
    }

    private static boolean conditionHolds(EntityCtx ctx, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        return loaded.condition().get().test(ctx);
    }

    private static void markActivated(PowerContainer holder, ResourceLocation powerId, long gameTime) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        impl.setAuxInt(powerId, stamp(gameTime));
    }

    private static void clearActivated(PowerContainer holder, ResourceLocation powerId) {
        if (holder instanceof PowerContainerImpl impl) impl.removeAux(powerId);
    }

    private static int stamp(long gameTime) {
        return Math.max(1, (int) (gameTime & TICK_MASK));
    }

    private static boolean onsetElapsed(PowerContainer holder, ResourceLocation powerId, Expression onsetDelay,
                                        Entity owner, long gameTime) {
        int onset = onsetDelay.evalInt(owner);
        if (onset <= 0) return true;
        int since = stamp(gameTime) - holder.getAuxIntOr(powerId, 0);
        return since < 0 || since >= onset;
    }
}
