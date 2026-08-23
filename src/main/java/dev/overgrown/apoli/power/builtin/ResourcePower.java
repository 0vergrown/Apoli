package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class ResourcePower extends PowerType<ResourcePower.Cfg> {

    public static final int WARN_SIZE = 65536;

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
        boolean persistent,
        int size
    ) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.INT_OR_EXPR.optionalFieldOf("min").forGetter(Cfg::min),
        Expression.INT_OR_EXPR.optionalFieldOf("max").forGetter(Cfg::max),
        Expression.INT_OR_EXPR.optionalFieldOf("start_value").forGetter(Cfg::startValue),
        HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Cfg::hudRender),
        Codec.BOOL.optionalFieldOf("enforce_limits", true).forGetter(Cfg::enforceLimits),
        Codec.BOOL.optionalFieldOf("retain_value", false).forGetter(Cfg::retainValue),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("min_action", EntityAction.CODEC).forGetter(Cfg::minAction),
        dev.overgrown.apoli.codec.LoggedOptionalField.of("max_action", EntityAction.CODEC).forGetter(Cfg::maxAction),
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
                + "roughly {} MB per holder.", powerId, cfg.size, (long) cfg.size * 4L / (1024L * 1024L));
        }
        if (cfg.size > 1) impl.trimAuxInts(powerId, cfg.size);
        if (impl.getAuxInt(powerId).isPresent()) return;
        impl.setAuxInt(powerId, clampedStart(cfg, holder, powerId));
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
            int[] table = impl.getAuxInts(powerId);
            if (table != null) {
                int min = currentMin(cfg, holder, powerId);
                int max = currentMax(cfg, holder, powerId);
                int limit = Math.min(table.length, cfg.size);
                boolean changed = false;
                for (int slot = 1; slot < limit; slot++) {
                    int value = clamp(table[slot], min, max, cfg);
                    if (value == table[slot]) continue;
                    table[slot] = value;
                    changed = true;
                }
                if (changed) impl.markDirty();
            }
        }
        OptionalInt cur = impl.getAuxInt(powerId);
        if (cur.isEmpty()) return;
        int curVal = cur.getAsInt();
        int min = currentMin(cfg, holder, powerId);
        int max = currentMax(cfg, holder, powerId);
        int clamped = clamp(curVal, min, max, cfg);
        if (clamped != curVal) {
            impl.setAuxInt(powerId, clamped);
            fireBoundaryActions(cfg, holder.rawOwner(), curVal, clamped, min, max);
        }
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        return readValue(holder, powerId);
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int value) {
        return writeValue(holder, powerId, value);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Cfg cfg, PowerContainer holder, boolean max) {
        return boundOf(holder, powerId, max);
    }

    @Override
    public int resourceSize(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        return cfg.size;
    }

    @Override
    public OptionalInt readResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot) {
        if (!holder.hasPower(powerId)) return OptionalInt.empty();
        if (slot < 0 || slot >= cfg.size) return OptionalInt.empty();
        if (slot == 0) return holder.getAuxInt(powerId);
        int[] table = holder.getAuxInts(powerId);
        if (table != null && slot < table.length) return OptionalInt.of(table[slot]);
        return OptionalInt.of(clampedStart(cfg, holder, powerId));
    }

    @Override
    public OptionalInt writeResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot, int value) {
        if (slot == 0) return writeValue(holder, powerId, value);
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalInt.empty();
        if (!holder.hasPower(powerId)) return OptionalInt.empty();
        if (slot < 0 || slot >= cfg.size) return OptionalInt.empty();
        int min = currentMin(cfg, holder, powerId);
        int max = currentMax(cfg, holder, powerId);
        int[] table = impl.auxIntsAtLeast(powerId, slot + 1, clampedStart(cfg, holder, powerId));
        table[0] = holder.getAuxIntOr(powerId, table[0]);
        int prev = table[slot];
        int target;
        if (cfg.retainValue && cfg.enforceLimits && (value < min || value > max)) {
            target = prev;
        } else {
            target = clamp(value, min, max, cfg);
        }
        if (target != prev) {
            table[slot] = target;
            impl.markDirty();
            fireBoundaryActions(cfg, holder.rawOwner(), prev, target, min, max);
        }
        return OptionalInt.of(target);
    }

    @Override
    public int resourceIndexOf(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int value) {
        if (!holder.hasPower(powerId)) return -1;
        if (holder.getAuxIntOr(powerId, value + 1) == value) return 0;
        int[] table = holder.getAuxInts(powerId);
        int limit = table == null ? 1 : Math.min(table.length, cfg.size);
        for (int slot = 1; slot < limit; slot++) {
            if (table[slot] == value) return slot;
        }
        if (limit < cfg.size && clampedStart(cfg, holder, powerId) == value) return limit;
        return -1;
    }

    public int evalStartValue(Cfg cfg, PowerContainer holder) {
        Expression start = cfg.startValue.orElseGet(() -> cfg.min.orElse(null));
        if (start == null) return 0;
        return start.evalIntWith(holder.rawOwner(), holder, 0);
    }

    public int currentMin(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.min.isEmpty()) return Integer.MIN_VALUE;
        Expression min = cfg.min.get();
        if (min.constantValue().isPresent()) return (int) Math.round(min.constantValue().getAsDouble());
        int cur = holder.getAuxIntOr(powerId, 0);
        return min.evalIntWith(holder.rawOwner(), holder, cur);
    }

    public int currentMax(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.max.isEmpty()) return Integer.MAX_VALUE;
        Expression max = cfg.max.get();
        if (max.constantValue().isPresent()) return (int) Math.round(max.constantValue().getAsDouble());
        int cur = holder.getAuxIntOr(powerId, 0);
        return max.evalIntWith(holder.rawOwner(), holder, cur);
    }

    public static int clamp(int value, int min, int max, Cfg cfg) {
        if (!cfg.enforceLimits) return value;
        int hi = Math.max(min, max);
        int lo = Math.min(min, max);
        return Math.min(hi, Math.max(lo, value));
    }

    public static OptionalInt boundOf(@Nullable PowerContainer holder, ResourceLocation powerId, boolean max) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalInt.empty();
        if (!(PowerTypeRegistry.get(loaded.typeId()) instanceof ResourcePower rp)) return OptionalInt.empty();
        if (!(loaded.config() instanceof Cfg cfg)) return OptionalInt.empty();
        Expression bound = (max ? cfg.max() : cfg.min()).orElse(null);
        if (bound == null) return OptionalInt.of(max ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        java.util.OptionalDouble constant = bound.constantValue();
        if (constant.isPresent()) return OptionalInt.of((int) Math.round(constant.getAsDouble()));
        if (holder == null) return OptionalInt.empty();
        return OptionalInt.of(max ? rp.currentMax(cfg, holder, powerId) : rp.currentMin(cfg, holder, powerId));
    }

    public static OptionalInt readValue(PowerContainer holder, ResourceLocation powerId) {
        if (!holder.hasPower(powerId)) return OptionalInt.empty();
        return holder.getAuxInt(powerId);
    }

    public static void onEntityLoad(PowerContainer holder, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return;
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (!(type instanceof ResourcePower rp)) return;
        if (!(loaded.config() instanceof Cfg cfg)) return;
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (cfg.size > 1) impl.trimAuxInts(powerId, cfg.persistent ? cfg.size : 0);
        boolean present = impl.getAuxInt(powerId).isPresent();
        if (present && cfg.persistent) return;
        impl.setAuxInt(powerId, rp.clampedStart(cfg, holder, powerId));
    }

    private int clampedStart(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        return clamp(evalStartValue(cfg, holder), currentMin(cfg, holder, powerId),
            currentMax(cfg, holder, powerId), cfg);
    }

    public static OptionalInt writeValue(PowerContainer holder, ResourceLocation powerId, int newValue) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalInt.empty();
        if (!holder.hasPower(powerId)) return OptionalInt.empty();
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null) return OptionalInt.empty();
        PowerType<?> type = PowerTypeRegistry.get(loaded.typeId());
        if (!(type instanceof ResourcePower rp)) return OptionalInt.empty();
        if (!(loaded.config() instanceof Cfg cfg)) return OptionalInt.empty();
        int prev = impl.getAuxInt(powerId).orElse(rp.evalStartValue(cfg, holder));
        int min = rp.currentMin(cfg, holder, powerId);
        int max = rp.currentMax(cfg, holder, powerId);
        int target;
        if (cfg.retainValue && cfg.enforceLimits && (newValue < min || newValue > max)) {
            target = prev;
        } else {
            target = clamp(newValue, min, max, cfg);
        }
        if (target != prev) {
            impl.setAuxInt(powerId, target);
            int[] table = impl.getAuxInts(powerId);
            if (table != null && table.length > 0) table[0] = target;
            rp.fireBoundaryActions(cfg, holder.rawOwner(), prev, target, min, max);
        }
        return OptionalInt.of(target);
    }

    private void fireBoundaryActions(Cfg cfg, Entity owner, int prev, int newVal, int min, int max) {
        if (owner == null) return;
        if (cfg.minAction.isEmpty() && cfg.maxAction.isEmpty()) return;
        if (!(owner.level() instanceof ServerLevel level)) return;
        if (newVal == min && prev != min) {
            cfg.minAction.ifPresent(a -> a.run(new EntityCtx(owner, level)));
        }
        if (newVal == max && prev != max) {
            cfg.maxAction.ifPresent(a -> a.run(new EntityCtx(owner, level)));
        }
    }
}
