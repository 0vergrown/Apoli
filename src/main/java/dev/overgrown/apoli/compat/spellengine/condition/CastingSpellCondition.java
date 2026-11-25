package dev.overgrown.apoli.compat.spellengine.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;

public final class CastingSpellCondition implements ConditionType<EntityCtx, SpellFilter> {

    @Override
    public MapCodec<SpellFilter> codec() {
        return SpellFilter.MAP_CODEC;
    }

    @Override
    public boolean test(SpellFilter filter, EntityCtx ctx) {
        if (!(ctx.raw() instanceof SpellCaster.Entity caster)) return false;
        SpellCast.Process process = caster.getSpellCastProcess();
        return process != null && filter.test(process.spell());
    }
}
