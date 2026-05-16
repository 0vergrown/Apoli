package dev.overgrown.apoli.condition.builtin.bientity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class BiEntityConditions {
    private BiEntityConditions() {}

    public static void register() {
        ConditionTypes.BI_ENTITY.register(Apoli.id("distance"), new DistanceCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("owner"), new OwnerCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("can_see"), new CanSeeCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("actor_condition"), new ActorCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("target_condition"), new TargetCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("both"), new BothCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("either"), new EitherCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("equal"), new EqualCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("undirected"), new UndirectedCondition());
    }
}