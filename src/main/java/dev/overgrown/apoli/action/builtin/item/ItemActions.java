package dev.overgrown.apoli.action.builtin.item;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.alias.AliasingOptions;

public final class ItemActions {
    private ItemActions() {}

    public static void register() {
        ActionTypes.ITEM.register(Apoli.id("consume"), new ConsumeAction());
        ActionTypes.ITEM.register(Apoli.id("damage"), new DamageItemAction());
        ActionTypes.ITEM.register(
            Apoli.id("holder_action"),
            new HolderActionItemAction(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("holder")).build()
        );
        ActionTypes.ITEM.register(Apoli.id("merge_nbt"), new MergeNbtItemAction());
        ActionTypes.ITEM.register(Apoli.id("modify"), new ModifyItemAction());
        ActionTypes.ITEM.register(Apoli.id("remove_enchantment"), new RemoveEnchantmentItemAction());

        ActionTypes.ITEM.register(Apoli.id("script"), new ScriptItemAction());

        ActionTypes.ITEM.register(Apoli.id("send_action"),
            new dev.overgrown.apoli.action.builtin.meta.SendActionMeta.Item(),
            dev.overgrown.apoli.alias.AliasingOptions.builder().addTypeAlias("shappoli:send_action").build());
    }
}
