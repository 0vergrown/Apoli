package dev.overgrown.apoli.compat.spellengine.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.spellengine.SpellDamage;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.compat.spellengine.SpellImpactTracker;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.DamageCtx;
import net.minecraft.core.Holder;
import net.spell_engine.api.spell.Spell;
import net.spell_power.api.SpellSchool;

public final class SpellDamageCondition implements ConditionType<DamageCtx, SpellFilter> {

    @Override
    public MapCodec<SpellFilter> codec() {
        return SpellFilter.MAP_CODEC;
    }

    @Override
    public boolean test(SpellFilter filter, DamageCtx ctx) {
        SpellSchool school = SpellDamage.schoolOf(ctx.source());
        if (school == null) return false;
        if (!filter.schools().isEmpty() && !filter.schools().contains(school.id)) return false;
        if (filter.spells().isEmpty()) return true;
        Holder<Spell> current = SpellImpactTracker.current();
        if (current == null) return false;
        for (int i = 0, n = filter.spells().size(); i < n; i++) {
            if (filter.spells().get(i).matches(current)) return true;
        }
        return false;
    }
}
