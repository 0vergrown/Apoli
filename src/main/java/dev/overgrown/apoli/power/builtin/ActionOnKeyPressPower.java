package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.data.Expression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class ActionOnKeyPressPower extends PowerType<ActionOnKeyPressPower.Config> {
    private static final int SLOT_HELD_TICKS = dev.overgrown.apoli.data.expr.ExprContext.slot("held_ticks");

    private final Map<CooldownKey, Integer> cooldowns = new HashMap<>();
    private final Map<CooldownKey, Long> pressedAt = new HashMap<>();

    public record Config(EntityAction entityAction, Optional<EntityAction> entityActionReleased, Expression cooldown,
                         HudRender hudRender, Key key) implements dev.overgrown.apoli.power.HudRendered {
        public boolean releases() {
            return entityActionReleased.isPresent();
        }
    }

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action_released", EntityAction.CODEC).forGetter(Config::entityActionReleased),
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(1)).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Config::key)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        if (cooldowns.isEmpty()) return;
        CooldownKey key = new CooldownKey(holder.rawOwner().getUUID(), powerId);
        Integer remaining = cooldowns.get(key);
        if (remaining == null) return;
        if (remaining <= 1) cooldowns.remove(key);
        else cooldowns.put(key, remaining - 1);
    }

    @Override
    public void tickStored(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        tick(powerId, cfg, holder);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.allPowers().contains(powerId)) {
            CooldownKey key = new CooldownKey(holder.rawOwner().getUUID(), powerId);
            cooldowns.remove(key);
            pressedAt.remove(key);
        }
    }

    public boolean tryActivate(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (!(owner.level() instanceof ServerLevel level)) return false;
        CooldownKey key = new CooldownKey(owner.getUUID(), powerId);
        if (cooldowns.getOrDefault(key, 0) > 0) return false;
        cfg.entityAction.run(new EntityCtx(owner, level));
        int ticks = PowerResources.cooldownTicks(cfg.cooldown, holder);
        if (ticks > 0) cooldowns.put(key, ticks);
        if (cfg.releases()) pressedAt.putIfAbsent(key, level.getGameTime());
        return true;
    }

    public boolean release(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean run) {
        if (pressedAt.isEmpty()) return false;
        Entity owner = holder.rawOwner();
        if (!(owner.level() instanceof ServerLevel level)) return false;
        Long pressed = pressedAt.remove(new CooldownKey(owner.getUUID(), powerId));
        if (pressed == null || !run || cfg.entityActionReleased.isEmpty()) return false;
        double previous = dev.overgrown.apoli.data.expr.ExprContext.push(SLOT_HELD_TICKS,
            Math.max(0L, level.getGameTime() - pressed));
        boolean attributed = dev.overgrown.apoli.attribution.PowerCause.push(owner, powerId);
        try {
            cfg.entityActionReleased.get().run(new EntityCtx(owner, level));
        } finally {
            if (attributed) dev.overgrown.apoli.attribution.PowerCause.pop();
            dev.overgrown.apoli.data.expr.ExprContext.pop(SLOT_HELD_TICKS, previous);
        }
        return true;
    }

    public List<ResourceLocation> pressedPowers(UUID entity) {
        if (pressedAt.isEmpty()) return List.of();
        List<ResourceLocation> out = null;
        for (CooldownKey key : pressedAt.keySet()) {
            if (!key.entity().equals(entity)) continue;
            if (out == null) out = new ArrayList<>(2);
            out.add(key.powerId());
        }
        return out == null ? List.of() : out;
    }

    public void forget(UUID entity) {
        if (pressedAt.isEmpty()) return;
        pressedAt.keySet().removeIf(key -> key.entity().equals(entity));
    }

    public int getCooldown(LivingEntity owner, ResourceLocation powerId) {
        return cooldowns.getOrDefault(new CooldownKey(owner.getUUID(), powerId), 0);
    }

    @Override

    public boolean isCooldown() {

        return true;

    }


    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) {
            return OptionalInt.of(PowerResources.clientCooldown(holder, powerId));
        }
        return OptionalInt.of(cooldowns.getOrDefault(new CooldownKey(owner.getUUID(), powerId), 0));
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) return OptionalInt.empty();
        int clamped = Math.max(0, Math.min(value, Math.max(PowerResources.cooldownTicks(cfg.cooldown, holder), 0)));
        CooldownKey key = new CooldownKey(owner.getUUID(), powerId);
        if (clamped <= 0) cooldowns.remove(key);
        else cooldowns.put(key, clamped);
        if (owner instanceof ServerPlayer player) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, clamped));
        }
        return OptionalInt.of(clamped);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown, holder), 0) : 0);
    }

    private record CooldownKey(UUID entity, ResourceLocation powerId) {}
}
