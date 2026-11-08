package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.dev.DevMode;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public class ResourcePower extends PowerType<ResourcePower.Cfg> {

    public static final int WARN_SIZE = 65536;
    public static final double UNBOUNDED_MIN = Integer.MIN_VALUE;
    public static final double UNBOUNDED_MAX = Integer.MAX_VALUE;

    private static final int MAX_ACTION_DEPTH = 64;
    private static final int DEV_REPORT_TICKS = 20;
    private static final ThreadLocal<int[]> ACTION_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private static final java.util.Set<ResourceLocation> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public record Cfg(
        Optional<Expression> min,
        Optional<Expression> max,
        Optional<Expression> startValue,
        HudRender hudRender,
        boolean enforceLimits,
        boolean retainValue,
        Optional<EntityAction> minAction,
        Optional<EntityAction> maxAction,
        List<OnChange> onChange,
        boolean persistent,
        int size
    ) implements dev.overgrown.apoli.power.HudRendered {}

    public record OnChange(
        Optional<Expression> value,
        List<Expression> values,
        Comparison comparison,
        Optional<EntityAction> entityAction
    ) {
        public static final Codec<OnChange> CODEC = RecordCodecBuilder.create(i -> i.group(
            Expression.DOUBLE_OR_EXPR.optionalFieldOf("value").forGetter(OnChange::value),
            Codec.list(Expression.DOUBLE_OR_EXPR).optionalFieldOf("values", List.of()).forGetter(OnChange::values),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.EQUAL).forGetter(OnChange::comparison),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(OnChange::entityAction)
        ).apply(i, OnChange::new));

        public boolean matches(double current, Entity owner) {
            if (entityAction.isEmpty()) return false;
            if (value.isEmpty() && values.isEmpty()) return true;
            if (value.isPresent() && comparison.compare(current, PowerResources.canonical(value.get().eval(owner, current)))) {
                return true;
            }
            for (int i = 0; i < values.size(); i++) {
                if (comparison.compare(current, PowerResources.canonical(values.get(i).eval(owner, current)))) return true;
            }
            return false;
        }
    }

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.DOUBLE_OR_EXPR.optionalFieldOf("min").forGetter(Cfg::min),
        Expression.DOUBLE_OR_EXPR.optionalFieldOf("max").forGetter(Cfg::max),
        Expression.DOUBLE_OR_EXPR.optionalFieldOf("start_value").forGetter(Cfg::startValue),
        HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Cfg::hudRender),
        Codec.BOOL.optionalFieldOf("enforce_limits", true).forGetter(Cfg::enforceLimits),
        Codec.BOOL.optionalFieldOf("retain_value", false).forGetter(Cfg::retainValue),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("min_action", EntityAction.CODEC).forGetter(Cfg::minAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("max_action", EntityAction.CODEC).forGetter(Cfg::maxAction),
        LoggedOptionalField.of("on_change", Codec.list(OnChange.CODEC), List.of()).forGetter(Cfg::onChange),
        Codec.BOOL.optionalFieldOf("persistent", true).forGetter(Cfg::persistent),
        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("size", 1).forGetter(Cfg::size)
    ).apply(i, Cfg::new));

    private static final MapCodec<Cfg> CFG_CODEC = AliasingMapCodec.wrap(INNER, Map.of(
        "default", "start_value",
        "positions", "size",
        "slots", "size")
    );

    @Override
    public MapCodec<Cfg> configCodec() {
        return CFG_CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (cfg.size > WARN_SIZE && WARNED.add(powerId)) {
            dev.overgrown.apoli.Apoli.LOGGER.warn(
                "[Apoli] {} declares a resource of {} slots. Slots are only allocated once they are written, "
                + "so this costs nothing until used, but writing near the top of that range will allocate "
                + "roughly {} MB per holder.", powerId, cfg.size, (long) cfg.size * 8L / (1024L * 1024L));
        }
        if (cfg.size > 1) impl.trimAuxDoubles(powerId, cfg.size);
        if (impl.hasAux(powerId)) return;
        impl.setAuxDouble(powerId, clampedStart(cfg, holder, powerId));
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (!holder.hasPower(powerId)) {
            impl.removeAux(powerId);
        }
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        if (!cfg.enforceLimits) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (cfg.size > 1) {
            double[] table = impl.getAuxDoubles(powerId);
            if (table != null) {
                double min = currentMin(cfg, holder, powerId);
                double max = currentMax(cfg, holder, powerId);
                int limit = Math.min(table.length, cfg.size);
                boolean changed = false;
                for (int slot = 1; slot < limit; slot++) {
                    double value = clamp(table[slot], min, max, cfg);
                    if (value == table[slot]) continue;
                    table[slot] = value;
                    changed = true;
                }
                if (changed) impl.markDirty();
            }
        }
        double current = impl.getAuxDoubleOr(powerId, Double.NaN);
        if (Double.isNaN(current)) return;
        double min = currentMin(cfg, holder, powerId);
        double max = currentMax(cfg, holder, powerId);
        double clamped = clamp(current, min, max, cfg);
        if (clamped != current) {
            impl.setAuxDouble(powerId, clamped);
            fireBoundaryActions(powerId, cfg, holder.rawOwner(), current, clamped, min, max);
        }
    }

    @Override
    public OptionalDouble readResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        return readValue(holder, powerId);
    }

    @Override
    public OptionalDouble writeResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder, double value) {
        return writeValue(holder, powerId, value);
    }

    @Override
    public OptionalDouble resourceBound(ResourceLocation powerId, Cfg cfg, PowerContainer holder, boolean max) {
        return boundOf(holder, powerId, max);
    }

    @Override
    public int resourceSize(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        return cfg.size;
    }

    @Override
    public OptionalDouble readResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot) {
        if (!holder.hasPower(powerId)) return OptionalDouble.empty();
        if (slot < 0 || slot >= cfg.size) return OptionalDouble.empty();
        if (slot == 0) return holder.getAuxDouble(powerId);
        double[] table = holder.getAuxDoubles(powerId);
        if (table != null && slot < table.length) return OptionalDouble.of(table[slot]);
        return OptionalDouble.of(clampedStart(cfg, holder, powerId));
    }

    @Override
    public OptionalDouble writeResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot, double value) {
        if (slot == 0) return writeValue(holder, powerId, value);
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalDouble.empty();
        if (!holder.hasPower(powerId)) return OptionalDouble.empty();
        if (slot < 0 || slot >= cfg.size) return OptionalDouble.empty();
        double min = currentMin(cfg, holder, powerId);
        double max = currentMax(cfg, holder, powerId);
        double[] table = impl.auxDoublesAtLeast(powerId, slot + 1, clampedStart(cfg, holder, powerId));
        table[0] = holder.getAuxDoubleOr(powerId, table[0]);
        double prev = table[slot];
        double wanted = PowerResources.canonical(value);
        double target;
        if (cfg.retainValue && cfg.enforceLimits && (wanted < min || wanted > max)) {
            target = prev;
        } else {
            target = clamp(wanted, min, max, cfg);
        }
        if (target != prev) {
            table[slot] = target;
            impl.markDirty();
            fireBoundaryActions(powerId, cfg, holder.rawOwner(), prev, target, min, max);
        }
        return OptionalDouble.of(target);
    }

    @Override
    public int resourceIndexOf(ResourceLocation powerId, Cfg cfg, PowerContainer holder, double value) {
        if (!holder.hasPower(powerId)) return -1;
        if (holder.getAuxDoubleOr(powerId, Double.NaN) == value) return 0;
        double[] table = holder.getAuxDoubles(powerId);
        int limit = table == null ? 1 : Math.min(table.length, cfg.size);
        for (int slot = 1; slot < limit; slot++) {
            if (table[slot] == value) return slot;
        }
        if (limit < cfg.size && clampedStart(cfg, holder, powerId) == value) return limit;
        return -1;
    }

    public double evalStartValue(Cfg cfg, PowerContainer holder) {
        Expression start = cfg.startValue.orElseGet(() -> cfg.min.orElse(null));
        if (start == null) return 0;
        return PowerResources.canonical(start.evalWith(holder.rawOwner(), holder, 0));
    }

    public double currentMin(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.min.isEmpty()) return UNBOUNDED_MIN;
        Expression min = cfg.min.get();
        if (min.constantValue().isPresent()) return PowerResources.canonical(min.constantValue().getAsDouble());
        double current = holder.getAuxDoubleOr(powerId, 0);
        return PowerResources.canonical(min.evalWith(holder.rawOwner(), holder, current));
    }

    public double currentMax(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.max.isEmpty()) return UNBOUNDED_MAX;
        Expression max = cfg.max.get();
        if (max.constantValue().isPresent()) return PowerResources.canonical(max.constantValue().getAsDouble());
        double current = holder.getAuxDoubleOr(powerId, 0);
        return PowerResources.canonical(max.evalWith(holder.rawOwner(), holder, current));
    }

    public static double clamp(double value, double min, double max, Cfg cfg) {
        if (!cfg.enforceLimits) return value;
        double hi = Math.max(min, max);
        double lo = Math.min(min, max);
        return Math.min(hi, Math.max(lo, value));
    }

    public static OptionalDouble boundOf(@Nullable PowerContainer holder, ResourceLocation powerId, boolean max) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalDouble.empty();
        if (!(PowerTypeRegistry.get(loaded.typeId()) instanceof ResourcePower rp)) return OptionalDouble.empty();
        if (!(loaded.config() instanceof Cfg cfg)) return OptionalDouble.empty();
        Expression bound = (max ? cfg.max() : cfg.min()).orElse(null);
        if (bound == null) return OptionalDouble.of(max ? UNBOUNDED_MAX : UNBOUNDED_MIN);
        java.util.OptionalDouble constant = bound.constantValue();
        if (constant.isPresent()) return OptionalDouble.of(PowerResources.canonical(constant.getAsDouble()));
        if (holder == null) return OptionalDouble.empty();
        return OptionalDouble.of(max ? rp.currentMax(cfg, holder, powerId) : rp.currentMin(cfg, holder, powerId));
    }

    public static OptionalDouble readValue(PowerContainer holder, ResourceLocation powerId) {
        if (!holder.hasPower(powerId)) return OptionalDouble.empty();
        return holder.getAuxDouble(powerId);
    }

    public static void onEntityLoad(PowerContainer holder, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (!(type instanceof ResourcePower rp)) return;
        if (!(loaded.config() instanceof Cfg cfg)) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        rp.resetOnLoad(powerId, cfg, impl);
    }

    protected void resetOnLoad(ResourceLocation powerId, Cfg cfg, PowerContainerImpl impl) {
        if (cfg.size > 1) impl.trimAuxDoubles(powerId, cfg.persistent ? cfg.size : 0);
        if (impl.hasAux(powerId) && cfg.persistent) return;
        impl.setAuxDouble(powerId, clampedStart(cfg, impl, powerId));
    }

    protected double clampedStart(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        return clamp(evalStartValue(cfg, holder), currentMin(cfg, holder, powerId),
            currentMax(cfg, holder, powerId), cfg);
    }

    public static OptionalDouble writeValue(PowerContainer holder, ResourceLocation powerId, double newValue) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalDouble.empty();
        if (!holder.hasPower(powerId)) return OptionalDouble.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalDouble.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (!(type instanceof ResourcePower rp)) return OptionalDouble.empty();
        if (!(loaded.config() instanceof Cfg cfg)) return OptionalDouble.empty();
        double prev = impl.getAuxDoubleOr(powerId, Double.NaN);
        if (Double.isNaN(prev)) prev = rp.evalStartValue(cfg, holder);
        double min = rp.currentMin(cfg, holder, powerId);
        double max = rp.currentMax(cfg, holder, powerId);
        double wanted = PowerResources.canonical(newValue);
        double target;
        if (cfg.retainValue && cfg.enforceLimits && (wanted < min || wanted > max)) {
            target = prev;
        } else {
            target = clamp(wanted, min, max, cfg);
        }
        if (target != prev) {
            impl.setAuxDouble(powerId, target);
            double[] table = impl.getAuxDoubles(powerId);
            if (table != null && table.length > 0) table[0] = target;
            rp.fireBoundaryActions(powerId, cfg, holder.rawOwner(), prev, target, min, max);
        }
        return OptionalDouble.of(target);
    }

    protected void fireBoundaryActions(ResourceLocation powerId, Cfg cfg, Entity owner, double prev, double newVal,
                                       double min, double max) {
        if (owner == null || newVal == prev) return;
        if (cfg.minAction.isEmpty() && cfg.maxAction.isEmpty() && cfg.onChange.isEmpty()) return;
        if (!(owner.level() instanceof ServerLevel level)) return;
        int[] depth = ACTION_DEPTH.get();
        if (depth[0] >= MAX_ACTION_DEPTH) {
            if (FunctionWarnings.first(powerId, "resource_actions")) {
                dev.overgrown.apoli.Apoli.LOGGER.warn("[Apoli] {} stopped running its min_action, max_action and on_change "
                    + "after {} changes in a row that each triggered another one. Check for an action that keeps "
                    + "changing the resource it was triggered by.", powerId, MAX_ACTION_DEPTH);
            }
            DevMode.reportEvery(owner, "stopped " + powerId, DEV_REPORT_TICKS, powerId + " stopped after " + MAX_ACTION_DEPTH
                + " chained changes: an action keeps changing the resource that triggered it");
            return;
        }
        StringBuilder fired = DevMode.any() ? new StringBuilder() : null;
        depth[0]++;
        try {
            if (newVal == min && prev != min && cfg.minAction.isPresent()) {
                cfg.minAction.get().run(EntityCtx.of(owner, level));
                if (fired != null) fired.append("min_action");
            }
            if (newVal == max && prev != max && cfg.maxAction.isPresent()) {
                cfg.maxAction.get().run(EntityCtx.of(owner, level));
                if (fired != null) fired.append(fired.isEmpty() ? "" : ", ").append("max_action");
            }
            List<OnChange> onChange = cfg.onChange;
            for (int i = 0; i < onChange.size(); i++) {
                OnChange entry = onChange.get(i);
                if (!entry.matches(newVal, owner)) continue;
                entry.entityAction().get().run(EntityCtx.of(owner, level));
                if (fired != null) fired.append(fired.isEmpty() ? "" : ", ").append("on_change[").append(i).append(']');
            }
        } finally {
            depth[0]--;
        }
        if (fired != null && !fired.isEmpty()) {
            DevMode.reportEvery(owner, powerId, DEV_REPORT_TICKS, powerId + " " + PowerResources.format(prev) + " → "
                + PowerResources.format(newVal) + " ran " + fired);
        }
    }
}
