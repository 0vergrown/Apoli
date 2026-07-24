package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.rope.RopeManager;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class DetachRopeAction implements ActionType<EntityCtx, DetachRopeAction.Cfg> {
    public record Cfg(Optional<String> slot) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("slot").forGetter(Cfg::slot)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity actor = ctx.raw();
        if (actor == null) return;
        RopeManager.removeMatching(actor.getUUID(), cfg.slot.orElse(null), null);
    }
}
