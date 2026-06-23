package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.ExpressionContext;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
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
        int initial = evalStartValue(cfg, holder.owner());
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
            fireBoundaryActions(cfg, holder.owner(), curVal, clamped, min, max);
        }
    }

    public int evalStartValue(Cfg cfg, LivingEntity entity) {
        Map<String, Double> vars = ExpressionContext.forResource(entity, 0);
        Map<String, Double> resources = ExpressionContext.resourceValues(getContainer(entity));
        return cfg.startValue.orElse(cfg.min).evalInt(vars, resources);
    }

    public int currentMin(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.min.constantValue().isPresent()) return (int) Math.round(cfg.min.constantValue().getAsDouble());
        int cur = readValue(holder, powerId).orElse(0);
        Map<String, Double> vars = ExpressionContext.forResource(holder.owner(), cur);
        Map<String, Double> resources = ExpressionContext.resourceValues(holder);
        return cfg.min.evalInt(vars, resources);
    }

    public int currentMax(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        if (cfg.max.constantValue().isPresent()) return (int) Math.round(cfg.max.constantValue().getAsDouble());
        int cur = readValue(holder, powerId).orElse(0);
        Map<String, Double> vars = ExpressionContext.forResource(holder.owner(), cur);
        Map<String, Double> resources = ExpressionContext.resourceValues(holder);
        return cfg.max.evalInt(vars, resources);
    }

    public static int clamp(int value, int min, int max, Cfg cfg) {
        if (!cfg.enforceLimits) return value;
        int hi = Math.max(min, max);
        int lo = Math.min(min, max);
        return Math.min(hi, Math.max(lo, value));
    }

    public static OptionalInt readValue(PowerContainer holder, ResourceLocation powerId) {
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
        int initial = rp.evalStartValue(cfg, holder.owner());
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
        int prev = impl.getAuxInt(powerId).orElse(rp.evalStartValue(cfg, holder.owner()));
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
            rp.fireBoundaryActions(cfg, holder.owner(), prev, target, min, max);
        }
        return OptionalInt.of(target);
    }

    private void fireBoundaryActions(Cfg cfg, LivingEntity owner, int prev, int newVal, int min, int max) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        if (newVal == min && prev != min) {
            cfg.minAction.ifPresent(a -> a.run(new EntityCtx(owner, level)));
        }
        if (newVal == max && prev != max) {
            cfg.maxAction.ifPresent(a -> a.run(new EntityCtx(owner, level)));
        }
    }

    private static @Nullable PowerContainer getContainer(LivingEntity entity) {
        return PowerContainer.of(entity);
    }
}
