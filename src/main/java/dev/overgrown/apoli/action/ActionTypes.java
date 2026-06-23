package dev.overgrown.apoli.action;

import dev.overgrown.apoli.action.builtin.bientity.BiEntityActions;
import dev.overgrown.apoli.action.builtin.block.BlockActions;
import dev.overgrown.apoli.action.builtin.entity.EntityActions;
import dev.overgrown.apoli.action.builtin.item.ItemActions;
import dev.overgrown.apoli.action.builtin.meta.MetaActions;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;

public final class ActionTypes {
    public static final TypedActionRegistry<EntityCtx> ENTITY = new TypedActionRegistry<>("entity");
    public static final TypedActionRegistry<BiEntityCtx> BI_ENTITY = new TypedActionRegistry<>("bi_entity");
    public static final TypedActionRegistry<BlockCtx> BLOCK = new TypedActionRegistry<>("block");
    public static final TypedActionRegistry<ItemCtx> ITEM = new TypedActionRegistry<>("item");

    private ActionTypes() {}

    public static void bootstrap() {
        EntityActions.register();
        BiEntityActions.register();
        BlockActions.register();
        ItemActions.register();
        MetaActions.registerEntity();
        MetaActions.registerBiEntity();
        MetaActions.registerBlock();
        MetaActions.registerItem();
    }
}