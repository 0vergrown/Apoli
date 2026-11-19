package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ActionOnAdvancementHandler {
    public static void run(ServerPlayer player, AdvancementHolder advancement) {
        EntityCtx ctx = EntityCtx.of(player, player.level());
        PowerLookup.forEachEntry(player, ApoliIds.ACTION_ON_ADVANCEMENT, ActionOnAdvancementPower.Config.class, (rl, cfg) -> {
            if ((cfg.advancement().isEmpty() || cfg.advancement().get().equals(advancement.id())) && ((cfg.category().isEmpty()) || cfg.category().get().equals(getRoot(player, advancement)))) cfg.entityAction().run(ctx);
        });
    }

    private static @Nullable ResourceLocation getRoot(ServerPlayer player, AdvancementHolder advancement) {
        AdvancementNode node = player.server.getAdvancements()
                .tree()
                .get(advancement);

        return node == null ? null : node.root().holder().id();
    }
}
