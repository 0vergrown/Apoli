package dev.overgrown.apoli.action.builtin.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class HolderActionItemAction implements ActionType<ItemCtx, HolderActionItemAction.Cfg> {
    public record Cfg(Optional<EntityAction> action, Optional<EntityAction> entityAction) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("action", EntityAction.CODEC).forGetter(Cfg::action),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Cfg::entityAction)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, ItemCtx ctx) {
        LivingEntity holder = ctx.holder();
        if (holder == null) return;
        EntityAction action = cfg.entityAction.orElse(cfg.action.orElse(null));
        if (action == null) return;
        action.run(new EntityCtx(holder, ctx.level()));
    }
}
