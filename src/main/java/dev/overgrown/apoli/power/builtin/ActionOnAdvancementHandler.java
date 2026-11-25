package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ActionOnAdvancementHandler {
    private ActionOnAdvancementHandler() {}

    public static void run(ServerPlayer player, Advancement advancement) {
        if (!PowerLookup.holdsType(player, ApoliIds.ACTION_ON_ADVANCEMENT)) return;
        EntityCtx ctx = EntityCtx.of(player, player.level());
        ResourceLocation id = advancement.getId();
        ResourceLocation root = advancement.getRoot().getId();
        PowerLookup.forEachEntry(player, ApoliIds.ACTION_ON_ADVANCEMENT, ActionOnAdvancementPower.Config.class, (powerId, cfg) -> {
            if (cfg.advancement().isPresent() && !cfg.advancement().get().equals(id)) return;
            if (cfg.category().isPresent() && !cfg.category().get().equals(root)) return;
            cfg.entityAction().run(ctx);
        });
    }
}
