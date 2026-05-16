package dev.overgrown.apoli.condition.builtin.entity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class EntityConditions {
    private EntityConditions() {}

    public static void register() {
        ConditionTypes.ENTITY.register(Apoli.id("entity_type"), new EntityTypeCondition());
        ConditionTypes.ENTITY.register(Apoli.id("invisible"), new InvisibleCondition());
        ConditionTypes.ENTITY.register(Apoli.id("sneaking"), new SneakingCondition());
        ConditionTypes.ENTITY.register(Apoli.id("status_effect"), new StatusEffectCondition());
    }
}