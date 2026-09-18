package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.expr.ExprContext;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.power.ApoliIds;
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

public final class ActionOnMouseMovementPower extends PowerType<ActionOnMouseMovementPower.Config> {

    public record Config(
        EntityAction entityAction,
        boolean up,
        boolean down,
        boolean left,
        boolean right,
        float threshold,
        boolean ignoreModifyCursorSpeed,
        Expression cooldown,
        HudRender hudRender
    ) {
        public boolean anyDirection() {
            return !up && !down && !left && !right;
        }

        public boolean accepts(float yaw, float pitch) {
            if (anyDirection()) return true;
            if (right && yaw > 0.0F) return true;
            if (left && yaw < 0.0F) return true;
            if (down && pitch > 0.0F) return true;
            return up && pitch < 0.0F;
        }

        public float magnitude(float yaw, float pitch) {
            if (anyDirection()) return Math.max(Math.abs(yaw), Math.abs(pitch));
            float best = 0.0F;
            if (right && yaw > 0.0F) best = Math.max(best, yaw);
            if (left && yaw < 0.0F) best = Math.max(best, -yaw);
            if (down && pitch > 0.0F) best = Math.max(best, pitch);
            if (up && pitch < 0.0F) best = Math.max(best, -pitch);
            return best;
        }
    }

    private static final int SLOT_MOUSE_X = ExprContext.slot("mouse_x");
    private static final int SLOT_MOUSE_Y = ExprContext.slot("mouse_y");

    private record StateKey(UUID entity, ResourceLocation powerId) {}

    private final Map<StateKey, Integer> cooldowns = new HashMap<>();

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Config::entityAction),
            Codec.BOOL.optionalFieldOf("up", false).forGetter(Config::up),
            Codec.BOOL.optionalFieldOf("down", false).forGetter(Config::down),
            Codec.BOOL.optionalFieldOf("left", false).forGetter(Config::left),
            Codec.BOOL.optionalFieldOf("right", false).forGetter(Config::right),
            Codec.FLOAT.optionalFieldOf("threshold", 0.0F).forGetter(Config::threshold),
            Codec.BOOL.optionalFieldOf("ignore_modify_cursor_speed", false).forGetter(Config::ignoreModifyCursorSpeed),
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(0)).forGetter(Config::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Config::hudRender)
        ).apply(i, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        StateKey key = new StateKey(holder.rawOwner().getUUID(), powerId);
        Integer left = cooldowns.get(key);
        if (left == null || left <= 0) return;
        cooldowns.put(key, left - 1);
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        cooldowns.remove(new StateKey(holder.rawOwner().getUUID(), powerId));
    }

    public void forget(UUID entity) {
        cooldowns.keySet().removeIf(key -> key.entity().equals(entity));
    }

    public static boolean anyWatching(@Nullable Entity entity) {
        if (entity == null) return false;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return false;
        List<ResourceLocation> powers = container.powersOfType(ApoliIds.ACTION_ON_MOUSE_MOVEMENT);
        for (int i = 0; i < powers.size(); i++) {
            if (!container.isSuppressed(powers.get(i))) return true;
        }
        return false;
    }

    public static void moved(@Nullable Entity entity, float rawYaw, float rawPitch) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) return;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.powersOfType(ApoliIds.ACTION_ON_MOUSE_MOVEMENT).isEmpty()) return;
        if (!(dev.overgrown.apoli.power.PowerTypeRegistry.get(ApoliIds.ACTION_ON_MOUSE_MOVEMENT)
            instanceof ActionOnMouseMovementPower type)) return;

        ModifyCursorSpeedPower.Multipliers multipliers = ModifyCursorSpeedPower.compute(entity);
        float scaledYaw = (float) (rawYaw * multipliers.x());
        float scaledPitch = (float) (rawPitch * multipliers.y());
        EntityCtx ctx = new EntityCtx(entity, level);

        PowerLookup.forEachEntry(entity, ApoliIds.ACTION_ON_MOUSE_MOVEMENT, Config.class, (powerId, cfg) ->
            type.feed(entity, container, powerId, cfg, ctx,
                cfg.ignoreModifyCursorSpeed() ? rawYaw : scaledYaw,
                cfg.ignoreModifyCursorSpeed() ? rawPitch : scaledPitch));
    }

    private void feed(Entity entity, PowerContainer container, ResourceLocation powerId, Config cfg,
                      EntityCtx ctx, float yaw, float pitch) {
        if (!cfg.accepts(yaw, pitch)) return;
        if (cfg.magnitude(yaw, pitch) < cfg.threshold()) return;

        StateKey key = new StateKey(entity.getUUID(), powerId);
        Integer remaining = cooldowns.get(key);
        if (remaining != null && remaining > 0) return;

        double previousX = ExprContext.push(SLOT_MOUSE_X, yaw);
        double previousY = ExprContext.push(SLOT_MOUSE_Y, pitch);
        try {
            cfg.entityAction().run(ctx);
        } finally {
            ExprContext.pop(SLOT_MOUSE_Y, previousY);
            ExprContext.pop(SLOT_MOUSE_X, previousX);
        }

        int ticks = Math.max(PowerResources.cooldownTicks(cfg.cooldown(), container), 0);
        cooldowns.put(key, ticks);
        if (entity instanceof ServerPlayer player) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, ticks));
        }
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) {
            return OptionalInt.of(PowerResources.clientCooldown(holder, powerId));
        }
        Integer left = cooldowns.get(new StateKey(owner.getUUID(), powerId));
        return OptionalInt.of(left == null ? 0 : Math.max(0, left));
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Config cfg, PowerContainer holder, int value) {
        Entity owner = holder.rawOwner();
        if (owner.level().isClientSide()) return OptionalInt.empty();
        int clamped = Math.max(0, Math.min(value, Math.max(PowerResources.cooldownTicks(cfg.cooldown(), holder), 0)));
        cooldowns.put(new StateKey(owner.getUUID(), powerId), clamped);
        if (owner instanceof ServerPlayer player) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(powerId, clamped));
        }
        return OptionalInt.of(clamped);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Config cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown(), holder), 0) : 0);
    }
}
