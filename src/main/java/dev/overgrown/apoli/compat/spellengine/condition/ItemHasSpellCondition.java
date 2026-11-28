package dev.overgrown.apoli.compat.spellengine.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;

// The item carries a matching spell: a spell book, scroll or spell weapon. With no spell or school, any spell counts.
public final class ItemHasSpellCondition implements ConditionType<ItemCtx, SpellFilter> {
    @Override
    public MapCodec<SpellFilter> codec() {
        return SpellFilter.MAP_CODEC;
    }

    @Override
    public boolean test(SpellFilter filter, ItemCtx ctx) {
        if (ctx.stack().isEmpty() || ctx.level() == null) return false;
        SpellContainer container = SpellContainerHelper.containerFromItemStack(ctx.stack());
        if (container == null || container.spell_ids() == null) return false;
        for (String raw : container.spell_ids()) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) continue;
            if (filter.isAny()) return true;
            Holder<Spell> spell = SpellEngineCompat.spell(ctx.level(), id);
            if (spell != null && filter.test(spell)) return true;
        }
        return false;
    }
}
