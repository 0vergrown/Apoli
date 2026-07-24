package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.PowerTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionOnUseHandler {
    private ActionOnUseHandler() {}

    public static InteractionResult fire(Player actor, Entity target, InteractionHand hand) {
        InteractionResult outcome = fireSide(actor, actor, target, hand, false);
        if (outcome != InteractionResult.PASS) return outcome;
        return fireSide(target, actor, target, hand, true);
    }

    private record Recent(long gameTime, int entity, int hand, InteractionResult result) {}
    private static final Map<UUID, Recent> RECENT = new ConcurrentHashMap<>();

    public static InteractionResult fireOncePerTick(Player actor, Entity target, InteractionHand hand) {
        long now = actor.level().getGameTime();
        UUID uuid = actor.getUUID();
        Recent prev = RECENT.get(uuid);
        if (prev != null && prev.gameTime() == now && prev.entity() == target.getId() && prev.hand() == hand.ordinal()) {
            return prev.result();
        }
        InteractionResult result = fire(actor, target, hand);
        RECENT.put(uuid, new Recent(now, target.getId(), hand.ordinal(), result));
        return result;
    }

    private static InteractionResult fireSide(Entity holder, Player actor, Entity target,
                                              InteractionHand hand, boolean targetUsedSide) {
        PowerContainer container = PowerContainer.of(holder);
        if (container == null) return InteractionResult.PASS;
        ResourceLocation onUseId = dev.overgrown.apoli.Apoli.id("action_on_use");
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (container.isSuppressed(powerId)) continue;
            if (!onUseId.equals(PowerTypeRegistry.resolveId(power.typeId()))) continue;
            if (!(power.config() instanceof ActionOnUsePower.Config cfg)) continue;
            if (cfg.targetUsed() != targetUsedSide) continue;
            if (power.condition().isPresent()
                && !power.condition().get().test(new dev.overgrown.apoli.condition.context.EntityCtx(holder, holder.level()))) {
                continue;
            }
            InteractionResult result = PowerTypes.ACTION_ON_USE.tryFire(cfg, actor, target, hand);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }
}
