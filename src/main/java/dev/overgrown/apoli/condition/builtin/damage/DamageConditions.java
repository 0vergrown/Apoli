package dev.overgrown.apoli.condition.builtin.damage;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionTypes;

public final class DamageConditions {
    private DamageConditions() {}

    public static void register() {
        ConditionTypes.DAMAGE.register(Apoli.id("amount"), new AmountDamageCondition());
        ConditionTypes.DAMAGE.register(
            Apoli.id("attack_charge"),
            new AttackChargeDamageCondition(),
            AliasingOptions.builder()
                .addTypeAlias(Apoli.id("spam_attack"))
                .addTypeAlias(Apoli.id("attack_cooldown"))
                .build()
        );
        ConditionTypes.DAMAGE.register(Apoli.id("attacker"), new AttackerDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("in_tag"), new InTagDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("name"), new NameDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("projectile"), new ProjectileDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("type"), new TypeDamageCondition());
    }
}
