package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.ScrollDirection;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class ActionOnScrollWheelPower extends PowerType<ActionOnScrollWheelPower.Config> {

    public record Config(
        EntityAction entityAction,
        ScrollDirection direction,
        int scrollAmount,
        int withinTicks,
        boolean resetOnDirectionChange,
        boolean preventHotbarChange,
        Expression cooldown,
        HudRender hudRender
    ) {}

    private static final class Progress {
        int notches;
        int idle;
        int cooldown;
        ScrollDirection last;
    }

    private final Map<StateKey, Progress> progress = new HashMap<>();

    private record StateKey(UUID entity, ResourceLocation powerId) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction),
            ScrollDirection.CODEC.optionalFieldOf("direction", ScrollDirection.ANY).forGetter(Config::direction),
            Codec.INT.optionalFieldOf("scroll_amount", 1).forGetter(Config::scrollAmount),
            Codec.INT.optionalFieldOf("within_ticks", 10).forGetter(Config::withinTicks),
            Codec.BOOL.optionalFieldOf("reset_on_direction_change", true).forGetter(Config::resetOnDirectionChange),
            Codec.BOOL.optionalFieldOf("prevent_hotbar_change", false).forGetter(Config::preventHotbarChange),
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(0)).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Progress state = progress.get(new StateKey(holder.rawOwner().getUUID(), powerId));
        if (state == null) return;
        if (state.cooldown > 0) state.cooldown--;
        if (state.notches <= 0) return;
        if (cfg.withinTicks <= 0) return;
        state.idle++;
        if (state.idle >= cfg.withinTicks) {
            state.notches = 0;
            state.idle = 0;
            state.last = null;
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        progress.remove(new StateKey(holder.rawOwner().getUUID(), powerId));
    }

    public void forget(UUID entity) {
        progress.keySet().removeIf(key -> key.entity().equals(entity));
    }

    public static boolean anyAccepting(@Nullable Entity entity, ScrollDirection notch) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ACTION_ON_SCROLL_WHEEL);
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof Config cfg)) continue;
            if (cfg.direction.accepts(notch)) return true;
        }
        return false;
    }

    public static int scroll(@Nullable Entity entity, ScrollDirection notch, int notches) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return 0;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return 0;
        if (container.powersOfType(ApoliIds.ACTION_ON_SCROLL_WHEEL).isEmpty()) return 0;

        ActionOnScrollWheelPower type = dev.overgrown.apoli.power.PowerTypeRegistry.get(ApoliIds.ACTION_ON_SCROLL_WHEEL)
            instanceof ActionOnScrollWheelPower t ? t : null;
        if (type == null) return 0;

        EntityCtx ctx = new EntityCtx(entity, level);
        int[] fired = new int[]{0};
        BiConsumer<ResourceLocation, Config> feeder = (powerId, cfg) -> {
            if (type.feed(entity, container, powerId, cfg, ctx, notch, notches)) fired[0]++;
        };
        PowerLookup.forEachEntry(entity, ApoliIds.ACTION_ON_SCROLL_WHEEL, Config.class, feeder);
        return fired[0];
    }

    private boolean feed(Entity entity, PowerContainer container, ResourceLocation powerId, Config cfg,
                         EntityCtx ctx, ScrollDirection notch, int notches) {
        if (!cfg.direction.accepts(notch)) return false;

        StateKey key = new StateKey(entity.getUUID(), powerId);
        Progress state = progress.computeIfAbsent(key, k -> new Progress());
        if (state.cooldown > 0) return false;

        if (cfg.resetOnDirectionChange && state.last != null && state.last != notch) state.notches = 0;
        state.last = notch;
        state.idle = 0;
        state.notches += Math.max(1, notches);

        int needed = Math.max(1, cfg.scrollAmount);
        if (state.notches < needed) return false;

        state.notches = 0;
        state.last = null;
        cfg.entityAction.run(ctx);

        int ticks = Math.max(PowerResources.cooldownTicks(cfg.cooldown, container), 0);
        state.cooldown = ticks;
        if (entity instanceof ServerPlayer player) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, ticks));
        }
        return true;
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) {
            return OptionalInt.of(PowerResources.clientCooldown(holder, powerId));
        }
        Progress state = progress.get(new StateKey(owner.getUUID(), powerId));
        return OptionalInt.of(state == null ? 0 : Math.max(0, state.cooldown));
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) return OptionalInt.empty();
        int clamped = Math.max(0, Math.min(value, Math.max(PowerResources.cooldownTicks(cfg.cooldown, holder), 0)));
        progress.computeIfAbsent(new StateKey(owner.getUUID(), powerId), k -> new Progress()).cooldown = clamped;
        if (owner instanceof ServerPlayer player) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, clamped));
        }
        return OptionalInt.of(clamped);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown, holder), 0) : 0);
    }
}
