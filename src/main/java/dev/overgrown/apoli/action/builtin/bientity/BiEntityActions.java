package dev.overgrown.apoli.action.builtin.bientity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.alias.AliasingOptions;

public final class BiEntityActions {
    private BiEntityActions() {}

    public static void register() {
        ActionTypes.BI_ENTITY.register(Apoli.id("add_velocity"), new AddVelocityAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("damage"), new DamageBiEntityAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("explode"), new ExplodeBiEntityAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("mount"), new MountAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("set_in_love"), new SetInLoveAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("tame"), new TameAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("actor_action"), new ActorAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("target_action"), new TargetAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("invert"), new InvertBiEntityAction());
        ActionTypes.BI_ENTITY.register(
            Apoli.id("add_to_entity_set"),
            new AddToEntitySetAction(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("add_to_set")).build()
        );
        ActionTypes.BI_ENTITY.register(
            Apoli.id("remove_from_entity_set"),
            new RemoveFromEntitySetAction(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("remove_from_set")).build()
        );
    }
}