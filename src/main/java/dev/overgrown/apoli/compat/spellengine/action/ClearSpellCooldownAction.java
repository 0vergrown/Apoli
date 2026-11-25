package dev.overgrown.apoli.compat.spellengine.action;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.compat.spellengine.SpellEngineCompat;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.cost.SpellCooldownManager;

import java.util.List;

public final class ClearSpellCooldownAction implements ActionType<EntityCtx, SpellFilter> {

    @Override
    public MapCodec<SpellFilter> codec() {
        return SpellFilter.MAP_CODEC;
    }

    @Override
    public void run(SpellFilter filter, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Player player) || player.level().isClientSide()) return;
        SpellCooldownManager cooldowns = ((SpellCaster.Player) player).getCooldownManager();
        if (filter.isAny()) {
            cooldowns.reset(null);
            return;
        }
        List<ResourceLocation> cooling = cooldowns.spellsOnCooldown();
        for (int i = 0, n = cooling.size(); i < n; i++) {
            ResourceLocation id = cooling.get(i);
            Holder<Spell> spell = SpellEngineCompat.spell(player.level(), id);
            if (spell == null || !filter.test(spell)) continue;
            cooldowns.remove(id);
            ResourceLocation group = SpellEngineCompat.cooldownGroup(spell);
            if (group != null) cooldowns.remove(group);
        }
    }
}
