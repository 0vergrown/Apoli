package dev.overgrown.apoli.condition.builtin.bientity;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
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
        ConditionTypes.BI_ENTITY.register(
            Apoli.id("in_entity_set"),
            new InEntitySetCondition(),
            AliasingOptions.builder().addTypeAlias(Apoli.id("in_set")).build()
        );
        ConditionTypes.BI_ENTITY.register(Apoli.id("riding"), new RidingBiEntityCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("riding_recursive"), new RidingRecursiveCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("riding_root"), new RidingRootCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("attacker"), new AttackerBiEntityCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("attack_target"), new AttackTargetCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("relative_rotation"), new RelativeRotationCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("invert"), new InvertBiEntityCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("disguised"), new DisguisedBiCondition());
        ConditionTypes.BI_ENTITY.register(Apoli.id("roped_together"), new RopedTogetherCondition());
    }
}