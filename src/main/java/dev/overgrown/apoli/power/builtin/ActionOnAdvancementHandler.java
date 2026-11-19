package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

public final class ActionOnAdvancementHandler {
    public static void run(ServerPlayer player, Advancement advancement) {
        EntityCtx ctx = EntityCtx.of(player, player.level());
        PowerLookup.forEachEntry(player, ApoliIds.ACTION_ON_ADVANCEMENT, ActionOnAdvancementPower.Config.class, (rl, cfg) -> {
            if ((cfg.advancement().isEmpty() || cfg.advancement().get().equals(advancement.getId())) && ((cfg.category().isEmpty()) || cfg.category().get().equals(advancement.getRoot().getId()))) cfg.entityAction().run(ctx);
        });
    }
}
