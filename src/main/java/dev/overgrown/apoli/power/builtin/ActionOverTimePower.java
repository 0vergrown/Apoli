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

import java.util.List;
import java.util.Optional;

public final class ActionOverTimePower extends PowerType<ActionOverTimePower.Config> {

    public static final ResourceLocation CANONICAL = dev.overgrown.apoli.Apoli.id("action_over_time");

    public record Step(
        int interval,
        Expression onsetDelay,
        EntityAction entityAction,
        Optional<EntityCondition> condition
    ) {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("interval", 20).forGetter(Step::interval),
            Expression.INT_OR_EXPR.optionalFieldOf("onset_delay", Expression.constant(0)).forGetter(Step::onsetDelay),
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Step::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.strict("condition", EntityCondition.CODEC).forGetter(Step::condition)
        ).apply(instance, Step::new));

        public int clampedInterval() {
            return interval > 0 ? interval : 1;
        }
    }

    public record Config(
        int interval,
        Expression onsetDelay,
        Optional<EntityAction> entityAction,
        Optional<EntityAction> risingAction,
        Optional<EntityAction> fallingAction,
        List<Step> steps,
        int stride
    ) {
        public int clampedInterval() {
            return interval > 0 ? interval : 1;
        }
    }

    private static final int TICK_MASK = 0x3FFFFFFF;

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("interval", 20).forGetter(Config::interval),
            Expression.INT_OR_EXPR.optionalFieldOf("onset_delay", Expression.constant(0)).forGetter(Config::onsetDelay),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("rising_action", EntityAction.CODEC).forGetter(Config::risingAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("falling_action", EntityAction.CODEC).forGetter(Config::fallingAction),
            Step.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Config::steps)
        ).apply(instance, ActionOverTimePower::build));
    }

    private static Config build(int interval, Expression onsetDelay, Optional<EntityAction> entityAction,
                                Optional<EntityAction> risingAction, Optional<EntityAction> fallingAction,
                                List<Step> steps) {
        int stride = interval > 0 ? interval : 1;
        for (int i = 0; i < steps.size(); i++) stride = gcd(stride, steps.get(i).clampedInterval());
        return new Config(interval, onsetDelay, entityAction, risingAction, fallingAction,
            List.copyOf(steps), Math.max(1, stride));
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
        int offset = Math.floorMod(tick - phase, stride);
        if (offset != 0) return;

        if (!(owner.level() instanceof ServerLevel level)) return;

        int interval = cfg.clampedInterval();
        boolean masterTick = Math.floorMod(tick - phase, interval) == 0;
        List<Step> steps = cfg.steps;
        boolean wasActive = holder.getAuxInt(powerId).orElse(0) != 0;

        if (!masterTick) {
            if (!wasActive || !anyStepDue(steps, tick, phase)) return;
            runSteps(steps, EntityCtx.of(owner, level), holder, powerId, owner, level.getGameTime(), tick, phase);
            return;
        }

        EntityCtx ctx = EntityCtx.of(owner, level);
        boolean active = conditionHolds(ctx, powerId);

        if (active) {
            if (!wasActive) {
                cfg.risingAction.ifPresent(a -> a.run(ctx));
                markActivated(holder, powerId, level.getGameTime());
            }
            long gameTime = level.getGameTime();
            if (onsetElapsed(holder, powerId, cfg.onsetDelay, owner, gameTime)) {
                cfg.entityAction.ifPresent(a -> a.run(ctx));
            }
            runSteps(steps, ctx, holder, powerId, owner, gameTime, tick, phase);
        } else if (wasActive) {
            cfg.fallingAction.ifPresent(a -> a.run(ctx));
            clearActivated(holder, powerId);
        }
    }

    private static boolean anyStepDue(List<Step> steps, int tick, int phase) {
        for (int i = 0; i < steps.size(); i++) {
            if (Math.floorMod(tick - phase, steps.get(i).clampedInterval()) == 0) return true;
        }
        return false;
    }

    private static void runSteps(List<Step> steps, EntityCtx ctx, PowerContainer holder, ResourceLocation powerId,
                                 Entity owner, long gameTime, int tick, int phase) {
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (Math.floorMod(tick - phase, step.clampedInterval()) != 0) continue;
            if (!onsetElapsed(holder, powerId, step.onsetDelay(), owner, gameTime)) continue;
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
