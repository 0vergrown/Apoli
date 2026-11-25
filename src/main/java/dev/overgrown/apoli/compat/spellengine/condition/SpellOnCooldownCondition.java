package dev.overgrown.apoli.compat.spellengine.condition;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.IdOrTag;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.cost.SpellCooldownManager;

import java.util.List;

public final class SpellOnCooldownCondition implements ConditionType<EntityCtx, SpellFilter> {

    @Override
    public MapCodec<SpellFilter> codec() {
        return SpellFilter.MAP_CODEC;
    }

    @Override
    public boolean test(SpellFilter filter, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Player player)) return false;
        SpellCooldownManager cooldowns = ((SpellCaster.Player) player).getCooldownManager();
        if (filter.schools().isEmpty() && filter.spells().size() == 1 && !filter.spells().get(0).isTag()) {
            IdOrTag<Spell> only = filter.spells().get(0);
            Holder<Spell> spell = SpellEngineCompat.spell(player.level(), only.id());
            return spell != null && cooldowns.isCoolingDown(spell);
        }
        List<ResourceLocation> cooling = cooldowns.spellsOnCooldown();
        for (int i = 0, n = cooling.size(); i < n; i++) {
            Holder<Spell> spell = SpellEngineCompat.spell(player.level(), cooling.get(i));
            if (spell != null && filter.test(spell)) return true;
        }
        return false;
    }
}
