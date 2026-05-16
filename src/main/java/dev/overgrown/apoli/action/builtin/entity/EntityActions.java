package dev.overgrown.apoli.action.builtin.entity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.action.ActionTypes;

public final class EntityActions {
    private EntityActions() {}

    public static void register() {
        ActionTypes.ENTITY.register(Apoli.id("apply_effect"), new ApplyEffectAction());
        ActionTypes.ENTITY.register(Apoli.id("execute_command"), new ExecuteCommandAction());
        ActionTypes.ENTITY.register(Apoli.id("exhaust"), new ExhaustAction());
        ActionTypes.ENTITY.register(Apoli.id("heal"), new HealAction());
        ActionTypes.ENTITY.register(Apoli.id("set_on_fire"), new SetOnFireAction());
    }
}