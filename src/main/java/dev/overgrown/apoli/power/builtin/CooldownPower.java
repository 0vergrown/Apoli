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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

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
        if (impl.getAuxInt(powerId).isPresent()) return;
        impl.setAuxInt(powerId, deadline(holder, startTicks(cfg, holder, powerId)));
    }

    @Override
    protected void resetOnLoad(ResourceLocation powerId, Cfg cfg, PowerContainerImpl impl) {
        if (impl.getAuxInt(powerId).isPresent() && cfg.persistent()) return;
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
                fireBoundaryActions(powerId, cfg, owner, remaining + 1, remaining, Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            return;
        }
        impl.setAuxInt(powerId, 0);
        fireBoundaryActions(powerId, cfg, owner, 1, 0, 0, length(cfg, holder, powerId));
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder) {
        if (!holder.hasPower(powerId)) return OptionalInt.empty();
        if (DevMode.isEnabled(holder.rawOwner())) return OptionalInt.of(0);
        int stored = holder.getAuxIntOr(powerId, 0);
        if (stored == 0) return OptionalInt.of(0);
        return OptionalInt.of(Math.max(0, stored - now(holder)));
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int value) {
        if (!(holder instanceof PowerContainerImpl impl)) return OptionalInt.empty();
        if (!holder.hasPower(powerId)) return OptionalInt.empty();
        int length = length(cfg, holder, powerId);
        int target = Math.max(0, Math.min(value, Math.max(length, 0)));
        int prev = readResource(powerId, cfg, holder).orElse(0);
        impl.setAuxInt(powerId, deadline(holder, target));
        if (target != prev) fireBoundaryActions(powerId, cfg, holder.rawOwner(), prev, target, 0, length);
        return OptionalInt.of(target);
    }

    @Override
    public OptionalInt readResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot) {
        return slot == 0 ? readResource(powerId, cfg, holder) : OptionalInt.empty();
    }

    @Override
    public OptionalInt writeResourceAt(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int slot, int value) {
        return slot == 0 ? writeResource(powerId, cfg, holder, value) : OptionalInt.empty();
    }

    @Override
    public int resourceIndexOf(ResourceLocation powerId, Cfg cfg, PowerContainer holder, int value) {
        OptionalInt current = readResource(powerId, cfg, holder);
        return current.isPresent() && current.getAsInt() == value ? 0 : -1;
    }

    private int length(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        return currentMax(cfg, holder, powerId);
    }

    private int startTicks(Cfg cfg, PowerContainer holder, ResourceLocation powerId) {
        int start = evalStartValue(cfg, holder);
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
