package dev.overgrown.apoli.action.builtin.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public final class SelfBiEntityAction implements ActionType<EntityCtx, SelfBiEntityAction.Cfg> {
    public record Cfg(BiEntityAction bientityAction) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(RecordCodecBuilder.mapCodec(i -> i.group(
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::bientityAction)
        ).apply(i, Cfg::new)), Map.of("action", "bientity_action"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity self = ctx.raw();
        if (self == null) return;
        cfg.bientityAction.run(new BiEntityCtx(self, self, ctx.level()));
    }
}
