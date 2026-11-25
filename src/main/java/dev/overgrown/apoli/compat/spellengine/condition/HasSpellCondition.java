package dev.overgrown.apoli.compat.spellengine.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.container.SpellContainerSource;

import java.util.List;

public final class HasSpellCondition implements ConditionType<EntityCtx, SpellFilter> {

    @Override
    public MapCodec<SpellFilter> codec() {
        return SpellFilter.MAP_CODEC;
    }

    @Override
    public boolean test(SpellFilter filter, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Player player)) return false;
        SpellContainerSource.Result spells = SpellContainerSource.getSpellsOf(player);
        return any(spells.actives(), filter) || any(spells.passives(), filter) || any(spells.modifiers(), filter);
    }

    private static boolean any(List<Holder<Spell>> spells, SpellFilter filter) {
        for (int i = 0, n = spells.size(); i < n; i++) {
            if (filter.test(spells.get(i))) return true;
        }
        return false;
    }
}
