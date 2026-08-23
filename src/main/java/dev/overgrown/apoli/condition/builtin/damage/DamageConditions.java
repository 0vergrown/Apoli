package dev.overgrown.apoli.condition.builtin.damage;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.alias.AliasingOptions;
import dev.overgrown.apoli.condition.ConditionTypes;
import net.minecraft.tags.DamageTypeTags;

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
        ConditionTypes.DAMAGE.register(Apoli.id("critical"), new CriticalDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("in_tag"), new InTagDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("name"), new NameDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("projectile"), new ProjectileDamageCondition());
        ConditionTypes.DAMAGE.register(Apoli.id("type"), new TypeDamageCondition());

        ConditionTypes.DAMAGE.register(Apoli.id("bypasses_armor"), new FixedTagDamageCondition(DamageTypeTags.BYPASSES_ARMOR));
        ConditionTypes.DAMAGE.register(Apoli.id("unblockable"), new FixedTagDamageCondition(DamageTypeTags.BYPASSES_SHIELD));
        ConditionTypes.DAMAGE.register(Apoli.id("out_of_world"), new FixedTagDamageCondition(DamageTypeTags.BYPASSES_INVULNERABILITY));
        ConditionTypes.DAMAGE.register(Apoli.id("explosive"), new FixedTagDamageCondition(DamageTypeTags.IS_EXPLOSION));
        ConditionTypes.DAMAGE.register(Apoli.id("fire"), new FixedTagDamageCondition(DamageTypeTags.IS_FIRE));
        ConditionTypes.DAMAGE.register(Apoli.id("from_falling"), new FixedTagDamageCondition(DamageTypeTags.IS_FALL));
    }
}
