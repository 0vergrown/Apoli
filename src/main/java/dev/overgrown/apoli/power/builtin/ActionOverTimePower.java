package dev.overgrown.apoli.power.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Expression;
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
        Expression onsetDelay,
        Optional<EntityAction> entityAction,
        Optional<EntityAction> risingAction,
        Optional<EntityAction> fallingAction
    ) {}

    private static final int TICK_MASK = 0x3FFFFFFF;

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("interval", 20).forGetter(Config::interval),
            Expression.INT_OR_EXPR.optionalFieldOf("onset_delay", Expression.constant(0)).forGetter(Config::onsetDelay),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("rising_action", EntityAction.CODEC).forGetter(Config::risingAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("falling_action", EntityAction.CODEC).forGetter(Config::fallingAction)
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
                markActivated(holder, powerId, level.getGameTime());
            }
            if (!onsetElapsed(holder, powerId, cfg, owner, level.getGameTime())) return;
            cfg.entityAction.ifPresent(a -> a.run(ctx));
        } else if (wasActive) {
            cfg.fallingAction.ifPresent(a -> a.run(ctx));
            clearActivated(holder, powerId);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Config cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        if (holder.getAuxInt(powerId).orElse(0) != 0
            && holder.rawOwner().level() instanceof ServerLevel level) {
            cfg.fallingAction.ifPresent(a -> a.run(EntityCtx.of(holder.rawOwner(), level)));
        }
        clearActivated(holder, powerId);
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

    private static void markActivated(PowerContainer holder, ResourceLocation powerId, long gameTime) {
        if (!(holder instanceof PowerContainerImpl impl)) return;
        impl.setAuxInt(powerId, stamp(gameTime));
    }

    private static void clearActivated(PowerContainer holder, ResourceLocation powerId) {
        if (holder instanceof PowerContainerImpl impl) impl.removeAux(powerId);
    }

    private static int stamp(long gameTime) {
        return Math.max(1, (int) (gameTime & TICK_MASK));
    }

    private static boolean onsetElapsed(PowerContainer holder, ResourceLocation powerId, Config cfg,
                                        Entity owner, long gameTime) {
        int onset = cfg.onsetDelay.evalInt(owner);
        if (onset <= 0) return true;
        int since = stamp(gameTime) - holder.getAuxIntOr(powerId, 0);
        return since < 0 || since >= onset;
    }
}
