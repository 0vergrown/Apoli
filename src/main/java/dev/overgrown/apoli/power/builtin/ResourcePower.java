package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
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

import java.util.Optional;
import java.util.OptionalInt;

public class ResourcePower extends PowerType<ResourcePower.Cfg> {

    public record Cfg(
        Expression min,
        Expression max,
        Optional<Expression> startValue,
        HudRender hudRender,
        boolean enforceLimits,
        boolean retainValue,
        Optional<EntityAction> minAction,
        Optional<EntityAction> maxAction,
        boolean persistent
    ) {}

    private static final MapCodec<Cfg> CFG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.INT_OR_EXPR.fieldOf("min").forGetter(Cfg::min),
        Expression.INT_OR_EXPR.fieldOf("max").forGetter(Cfg::max),
        Expression.INT_OR_EXPR.optionalFieldOf("start_value").forGetter(Cfg::startValue),
        HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Cfg::hudRender),
        Codec.BOOL.optionalFieldOf("enforce_limits", true).forGetter(Cfg::enforceLimits),
        Codec.BOOL.optionalFieldOf("retain_value", false).forGetter(Cfg::retainValue),
        EntityAction.CODEC.optionalFieldOf("min_action").forGetter(Cfg::minAction),
        EntityAction.CODEC.optionalFieldOf("max_action").forGetter(Cfg::maxAction),
        Codec.BOOL.optionalFieldOf("persistent", true).forGetter(Cfg::persistent)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CFG_CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.getAuxInt(powerId).isPresent()) return;
        int initial = evalStartValue(cfg, holder);
        impl.setAuxInt(powerId, clamp(initial, currentMin(cfg, holder, powerId), currentMax(cfg, holder, powerId), cfg));
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
        if (!(holder instanceof PowerContainerImpl impl)) return;
        OptionalInt cur = impl.getAuxInt(powerId);
        if (cur.isEmpty()) return;
        int curVal = cur.getAsInt();
        int min = currentMin(cfg, holder, powerId);
        int max = currentMax(cfg, holder, powerId);
        int clamped = cfg.enforceLimits ? clamp(curVal, min, max, cfg) : curVal;
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

    public int evalStartValue(Cfg cfg, PowerContainer holder) {
        return cfg.startValue.orElse(cfg.min).evalIntWith(holder.rawOwner(), holder, 0);
    }

    public int currentMin(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.min.constantValue().isPresent()) return (int) Math.round(cfg.min.constantValue().getAsDouble());
        int cur = holder.getAuxIntOr(powerId, 0);
        return cfg.min.evalIntWith(holder.rawOwner(), holder, cur);
    }

    public int currentMax(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.max.constantValue().isPresent()) return (int) Math.round(cfg.max.constantValue().getAsDouble());
        int cur = holder.getAuxIntOr(powerId, 0);
        return cfg.max.evalIntWith(holder.rawOwner(), holder, cur);
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
        Expression bound = max ? cfg.max() : cfg.min();
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
        boolean present = impl.getAuxInt(powerId).isPresent();
        if (present && cfg.persistent) return;
        int initial = rp.evalStartValue(cfg, holder);
        int min = rp.currentMin(cfg, holder, powerId);
        int max = rp.currentMax(cfg, holder, powerId);
        impl.setAuxInt(powerId, clamp(initial, min, max, cfg));
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
