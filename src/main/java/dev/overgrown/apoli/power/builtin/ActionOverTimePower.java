package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class ActionOverTimePower extends PowerType<ActionOverTimePower.Config> {

    public static final ResourceLocation CANONICAL = dev.overgrown.apoli.Apoli.id("action_over_time");

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
    public boolean ticksNonLivingEntities() {
        return true;
    }

    @Override
    public void tick(ResourceLocation powerId, Config cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();

        int interval = cfg.interval > 0 ? cfg.interval : 1;
        int phase = Math.floorMod(powerId.hashCode() + owner.getId(), interval);
        if (owner.tickCount % interval != phase) return;

        if (!(owner.level() instanceof ServerLevel level)) return;

        EntityCtx ctx = EntityCtx.of(owner, level);
        boolean wasActive = holder.getAuxInt(powerId).orElse(0) != 0;
        boolean active = conditionHolds(ctx, powerId);

        if (active) {
            if (!wasActive) {
                cfg.risingAction.ifPresent(a -> a.run(ctx));
                setActiveFlag(holder, powerId, true);
            }
            cfg.entityAction.ifPresent(a -> a.run(ctx));
        } else if (wasActive) {
            cfg.fallingAction.ifPresent(a -> a.run(ctx));
            setActiveFlag(holder, powerId, false);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        if (holder.getAuxInt(powerId).orElse(0) != 0
            && holder.rawOwner().level() instanceof ServerLevel level) {
            cfg.fallingAction.ifPresent(a -> a.run(EntityCtx.of(holder.rawOwner(), level)));
        }
        setActiveFlag(holder, powerId, false);
    }

    public static void resetEdges(Entity entity) {
        PowerContainer container = PowerContainer.of(entity);
        if (!(container instanceof PowerContainerImpl impl) || impl.isEmpty()) return;
        java.util.List<ResourceLocation> powers = container.powersOfType(CANONICAL);
        for (int i = 0; i < powers.size(); i++) {
            impl.removeAux(powers.get(i));
        }
    }

    private static boolean conditionHolds(EntityCtx ctx, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        return loaded.condition().get().test(ctx);
    }

    private static void setActiveFlag(PowerContainer holder, ResourceLocation powerId, boolean activeNow) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        if (activeNow) {
            impl.setAuxInt(powerId, 1);
        } else {
            impl.removeAux(powerId);
        }
    }
}
