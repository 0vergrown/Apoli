package dev.overgrown.apoli.compat.spellengine.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.compat.spellengine.SpellFilter;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;

public final class PreventSpellCastPower extends PowerType<SpellFilter> {
    public static final ResourceLocation CANONICAL = Apoli.id("prevent_spell_cast");

    @Override
    public MapCodec<SpellFilter> configCodec() {
        return SpellFilter.MAP_CODEC;
    }

    public static boolean prevents(Player caster, Holder<Spell> spell) {
        return PowerLookup.holdsType(caster, CANONICAL)
            && PowerLookup.anyActive(caster, CANONICAL, SpellFilter.class, filter -> filter.test(spell));
    }
}
