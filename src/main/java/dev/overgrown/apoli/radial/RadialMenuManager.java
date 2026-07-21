package dev.overgrown.apoli.radial;

import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RadialMenuManager {

    private record Pending(int nonce, List<EntityAction> actions) {}

    private static final Map<UUID, Pending> OPEN = new ConcurrentHashMap<>();
    private static final AtomicInteger NONCE = new AtomicInteger();

    private RadialMenuManager() {}

    public static int open(ServerPlayer player, List<EntityAction> actions) {
        int nonce = NONCE.incrementAndGet();
        OPEN.put(player.getUUID(), new Pending(nonce, actions));
        return nonce;
    }

    public static void select(ServerPlayer player, int nonce, int index) {
        Pending pending = OPEN.get(player.getUUID());
        if (pending == null || pending.nonce != nonce) return;
        OPEN.remove(player.getUUID());
        if (index < 0 || index >= pending.actions.size()) return;
        pending.actions.get(index).run(new EntityCtx(player, player.level()));
    }

    public static void forget(UUID player) {
        OPEN.remove(player);
    }
}
