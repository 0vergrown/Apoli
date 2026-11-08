package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.dev.DevMode;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class CooldownPower extends ResourcePower {

    private static final MapCodec<Cfg> CFG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Expression.INT_OR_EXPR.fieldOf("cooldown").forGetter(cfg -> cfg.max().orElse(Expression.constant(0))),
        HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Cfg::hudRender),
        Codec.BOOL.optionalFieldOf("persistent", true).forGetter(Cfg::persistent),
        Expression.INT_OR_EXPR.optionalFieldOf("start_value", Expression.constant(0))
            .forGetter(cfg -> cfg.startValue().orElse(Expression.constant(0))),
        LoggedOptionalField.of("min_action", EntityAction.CODEC).forGetter(Cfg::minAction),
        LoggedOptionalField.of("max_action", EntityAction.CODEC).forGetter(Cfg::maxAction),
        LoggedOptionalField.of("on_change", Codec.list(OnChange.CODEC), List.of()).forGetter(Cfg::onChange)
    ).apply(i, (cooldown, hud, persistent, start, minAction, maxAction, onChange) -> new Cfg(
        Optional.of(Expression.constant(0)),
        Optional.of(cooldown),
        Optional.of(start),
        hud,
        true,
        false,
        minAction,
        maxAction,
        onChange,
        persistent,
        1
    )));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CFG_CODEC;
    }

    @Override
    public boolean isCooldown() {
        return true;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (impl.hasAux(powerId)) return;
        impl.setAuxInt(powerId, deadline(holder, startTicks(cfg, holder, powerId)));
    }

    @Override
    protected void resetOnLoad(ResourceLocation powerId, Cfg cfg, PowerContainerImpl impl) {
        if (impl.hasAux(powerId) && cfg.persistent()) return;
        impl.setAuxInt(powerId, deadline(impl, startTicks(cfg, impl, powerId)));
    }

    @Override
    public void tick(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        int stored = impl.getAuxIntOr(powerId, 0);
        if (stored == 0) return;
        Entity owner = holder.rawOwner();
        int remaining = stored - now(holder);
        if (remaining > 0) {
            if (!cfg.onChange().isEmpty()) {
                fireBoundaryActions(powerId, cfg, owner, remaining + 1, remaining, UNBOUNDED_MIN, UNBOUNDED_MAX);
            }
            return;
        }
        impl.setAuxInt(powerId, 0);
        fireBoundaryActions(powerId, cfg, owner, 1, 0, 0, length(cfg, holder, powerId));
    }

    @Override
    public OptionalDouble readResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        if (!holder.hasPower(powerId)) return OptionalDouble.empty();
        if (DevMode.isEnabled(holder.rawOwner())) return OptionalDouble.of(0);
        int stored = holder.getAuxIntOr(powerId, 0);
        if (stored == 0) return OptionalDouble.of(0);
        return OptionalDouble.of(Math.max(0, stored - now(holder)));
    }

    @Override
    public OptionalDouble writeResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder, double value) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalDouble.empty();
        if (!holder.hasPower(powerId)) return OptionalDouble.empty();
        int length = length(cfg, holder, powerId);
        int target = Math.max(0, Math.min(PowerResources.whole(value), Math.max(length, 0)));
        int prev = (int) readResource(powerId, cfg, holder).orElse(0);
        impl.setAuxInt(powerId, deadline(holder, target));
        if (target != prev) fireBoundaryActions(powerId, cfg, holder.rawOwner(), prev, target, 0, length);
        return OptionalDouble.of(target);
    }

    @Override
    public OptionalDouble readResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot) {
        return slot == 0 ? readResource(powerId, cfg, holder) : OptionalDouble.empty();
    }

    @Override
    public OptionalDouble writeResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot, double value) {
        return slot == 0 ? writeResource(powerId, cfg, holder, value) : OptionalDouble.empty();
    }

    @Override
    public int resourceIndexOf(ResourceLocation powerId, Cfg cfg, PowerContainer holder, double value) {
        OptionalDouble current = readResource(powerId, cfg, holder);
        return current.isPresent() && current.getAsDouble() == value ? 0 : -1;
    }

    private int length(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        return PowerResources.whole(currentMax(cfg, holder, powerId));
    }

    private int startTicks(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        int start = PowerResources.whole(evalStartValue(cfg, holder));
        return Math.max(0, Math.min(start, Math.max(length(cfg, holder, powerId), 0)));
    }

    private static int deadline(PowerContainer holder, int ticks) {
        if (ticks <= 0) return 0;
        int at = now(holder) + ticks;
        return at == 0 ? 1 : at;
    }

    private static int now(PowerContainer holder) {
        return (int) holder.rawOwner().level().getGameTime();
    }
}
