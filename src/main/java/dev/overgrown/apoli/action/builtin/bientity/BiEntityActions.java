package dev.overgrown.apoli.action.builtin.bientity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionTypes;

public final class BiEntityActions {
    private BiEntityActions() {}

    public static void register() {
        ActionTypes.BI_ENTITY.register(Apoli.id("add_velocity"), new AddVelocityAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("tame"), new TameAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("actor_action"), new ActorAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("target_action"), new TargetAction());
        ActionTypes.BI_ENTITY.register(Apoli.id("invert"), new InvertBiEntityAction());
    }
}