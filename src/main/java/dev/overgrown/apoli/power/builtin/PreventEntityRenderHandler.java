package dev.overgrown.apoli.power.builtin;

import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliIds;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.Entity;

public final class PreventEntityRenderHandler {
    private PreventEntityRenderHandler() {}

    public static boolean shouldHide(Entity viewer, Entity rendered) {
        if (viewer == null || rendered == null) return false;
        boolean[] hide = {false};
        PowerLookup.forEach(viewer, ApoliIds.PREVENT_ENTITY_RENDER,
            PreventEntityRenderPower.Config.class, cfg -> {
                if (hide[0]) return;
                if (cfg.entityCondition().isPresent()
                    && !cfg.entityCondition().get().test(EntityCtx.of(rendered, rendered.level()))) return;
                if (cfg.bientityCondition().isPresent()
                    && !cfg.bientityCondition().get().test(new BiEntityCtx(viewer, rendered, viewer.level()))) return;
                hide[0] = true;
            });
        return hide[0];
    }
}
