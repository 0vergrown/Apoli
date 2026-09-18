package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PreventUseHandler {
    private PreventUseHandler() {}

    private record Claim(long gameTime, int entity, int hand) {}

    private static final Map<UUID, Claim> CLAIMED = new ConcurrentHashMap<>();

    public static boolean isPrevented(Player actor, Entity target, InteractionHand hand) {
        if (actor == null || target == null) return false;
        boolean runActions = !actor.level().isClientSide() && claim(actor, target, hand);
        return prevented(actor, actor, target, hand, false, runActions)
            || prevented(target, actor, target, hand, true, runActions);
    }

    private static boolean claim(Player actor, Entity target, InteractionHand hand) {
        Claim next = new Claim(actor.level().getGameTime(), target.getId(), hand.ordinal());
        return !next.equals(CLAIMED.put(actor.getUUID(), next));
    }

    private static boolean prevented(Entity holder, Player actor, Entity target, InteractionHand hand,
                                     boolean targetUsedSide, boolean runActions) {
        boolean[] blocked = {false};
        PowerLookup.forEach(holder, ApoliIds.PREVENT_USE, PreventUsePower.Config.class, cfg -> {
            if (blocked[0] || cfg.targetUsed() != targetUsedSide) return;
            blocked[0] = PowerTypes.PREVENT_USE.tryPrevent(cfg, actor, target, hand, runActions);
        });
        return blocked[0];
    }
}
