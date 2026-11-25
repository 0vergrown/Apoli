package dev.overgrown.apoli.compat.spellengine;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SpellDamage {
    private SpellDamage() {}

    private static volatile Map<ResourceKey<DamageType>, SpellSchool> byDamageType;
    private static volatile int knownSchools = -1;

    public static @Nullable SpellSchool schoolOf(DamageSource source) {
        Optional<ResourceKey<DamageType>> key = source.typeHolder().unwrapKey();
        if (key.isEmpty()) return null;
        return index().get(key.get());
    }

    private static Map<ResourceKey<DamageType>, SpellSchool> index() {
        Map<ResourceKey<DamageType>, SpellSchool> current = byDamageType;
        int schools = SpellSchools.all().size();
        if (current != null && knownSchools == schools) return current;
        Map<ResourceKey<DamageType>, SpellSchool> built = new HashMap<>();
        for (SpellSchool school : SpellSchools.all()) {
            if (school.damageType != null) built.putIfAbsent(school.damageType, school);
        }
        byDamageType = built;
        knownSchools = schools;
        return built;
    }
}
