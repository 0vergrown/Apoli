package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ActionOverTimePower extends PowerType<ActionOverTimePower.Config> {
    private final Map<StateKey, State> states = new HashMap<>();

    public record Config(
        int interval,
        Optional<EntityAction> entityAction,
        Optional<EntityAction> risingAction,
        Optional<EntityAction> fallingAction
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("interval", 20).forGetter(Config::interval),
            EntityAction.CODEC.optionalFieldOf("entity_action").forGetter(Config::entityAction),
            EntityAction.CODEC.optionalFieldOf("rising_action").forGetter(Config::risingAction),
            EntityAction.CODEC.optionalFieldOf("falling_action").forGetter(Config::fallingAction)
        ).apply(instance, Config::new));
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        LivingEntity owner = holder.owner();
        if (!(owner.level() instanceof ServerLevel level)) return;
        EntityCtx ctx = new EntityCtx(owner, level);
        StateKey key = new StateKey(owner.getUUID(), powerId);
        State state = states.computeIfAbsent(key, k -> new State());
        state.tickCount++;
        if (cfg.interval > 0 && state.tickCount % cfg.interval == 0) {
            cfg.entityAction.ifPresent(a -> a.run(ctx));
        }
        if (!state.previouslyActive) {
            cfg.risingAction.ifPresent(a -> a.run(ctx));
            state.previouslyActive = true;
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (!holder.allPowers().contains(powerId)) {
            StateKey key = new StateKey(holder.owner().getUUID(), powerId);
            State state = states.remove(key);
            if (state != null && state.previouslyActive
                && holder.owner().level() instanceof ServerLevel level) {
                cfg.fallingAction.ifPresent(a -> a.run(new EntityCtx(holder.owner(), level)));
            }
        }
    }

    private record StateKey(UUID entity, ResourceLocation powerId) {}

    private static final class State {
        long tickCount;
        boolean previouslyActive;
    }
}
