package dev.overgrown.apoli.action.builtin.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.entity.GrabManager;
import net.minecraft.world.entity.Entity;

public final class GrabAction implements ActionType<BiEntityCtx, GrabAction.Cfg> {

    public record Cfg(int duration, double distance, boolean disableGrabber, boolean disableGrabbed,
                      boolean horizontalOnly, boolean verticalOnly) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("duration", -1).forGetter(Cfg::duration),
            Codec.DOUBLE.optionalFieldOf("distance", 2.0).forGetter(Cfg::distance),
            Codec.BOOL.optionalFieldOf("disable_grabber", false).forGetter(Cfg::disableGrabber),
            Codec.BOOL.optionalFieldOf("disable_grabbed", false).forGetter(Cfg::disableGrabbed),
            Codec.BOOL.optionalFieldOf("horizontal_only", false).forGetter(Cfg::horizontalOnly),
            Codec.BOOL.optionalFieldOf("vertical_only", false).forGetter(Cfg::verticalOnly)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();
        if (actor == null || target == null || actor.level().isClientSide()) return;
        GrabManager.start(actor, target, cfg.duration(), cfg.distance(), cfg.disableGrabber(), cfg.disableGrabbed(),
            cfg.horizontalOnly(), cfg.verticalOnly());
    }
}
