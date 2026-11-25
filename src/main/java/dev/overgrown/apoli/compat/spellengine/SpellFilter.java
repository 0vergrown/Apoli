package dev.overgrown.apoli.compat.spellengine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.codec.SingleOrList;
import dev.overgrown.apoli.data.IdOrTag;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_power.api.SpellSchool;

import java.util.List;
import java.util.Optional;

public record SpellFilter(List<IdOrTag<Spell>> spells, List<ResourceLocation> schools) {

    public static final SpellFilter ANY = new SpellFilter(List.of(), List.of());

    public static final MapCodec<SpellFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        LoggedOptionalField.strict("spell", SingleOrList.of(IdOrTag.codec(SpellRegistry.KEY)))
            .xmap(o -> o.orElse(List.of()), (List<IdOrTag<Spell>> l) -> l.isEmpty() ? Optional.empty() : Optional.of(l))
            .forGetter(SpellFilter::spells),
        LoggedOptionalField.strict("school", SingleOrList.of(IdCodecs.ID))
            .xmap(o -> o.orElse(List.of()), (List<ResourceLocation> l) -> l.isEmpty() ? Optional.empty() : Optional.of(l))
            .forGetter(SpellFilter::schools)
    ).apply(i, SpellFilter::new));

    public boolean isAny() {
        return spells.isEmpty() && schools.isEmpty();
    }

    public boolean test(Holder<Spell> spell) {
        if (!spells.isEmpty()) {
            boolean listed = false;
            for (int i = 0, n = spells.size(); i < n; i++) {
                if (spells.get(i).matches(spell)) {
                    listed = true;
                    break;
                }
            }
            if (!listed) return false;
        }
        if (!schools.isEmpty()) {
            SpellSchool school = spell.value().school;
            return school != null && schools.contains(school.id);
        }
        return true;
    }
}
