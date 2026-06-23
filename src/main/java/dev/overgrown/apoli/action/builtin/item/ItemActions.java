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
    }
}
